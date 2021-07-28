package com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt;

import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.get.GetConfigurationsUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.mercadolibre.planning.model.api.util.DateUtils.ignoreMinutes;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType.CPT;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType.DEFERRAL;
import static java.lang.Math.min;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.iterate;

@Service
@AllArgsConstructor
public class CalculateCptProjectionUseCase {

    private static final int HOUR_IN_MINUTES = 60;

    private final GetConfigurationsUseCase getConfigurationsUseCase;
    private final GetCycleTimeUseCase getCycleTimeUseCase;

    public List<CptProjectionOutput> execute(final CptProjectionInput input) {
        final Map<ZonedDateTime, Map<ZonedDateTime, Integer>> unitsByDateOutAndDate =
                getUnitsByDateOutAndDate(input);

        final Map<ZonedDateTime, Integer> capacity = input.getCapacity();
        adaptMinutesFirstCapacity(capacity);
        return project(capacity, unitsByDateOutAndDate, input);
    }

    // TODO: Refactor of nested if statements
    @SuppressWarnings({"PMD.NullAssignment", "PMD.AvoidDeeplyNestedIfStmts"})
    private List<CptProjectionOutput> project(
            final Map<ZonedDateTime, Integer> capacityByDate,
            final Map<ZonedDateTime, Map<ZonedDateTime, Integer>> unitsByDateOutAndDate,
            final CptProjectionInput input) {

        final List<CptProjectionOutput> cptProjectionOutputs = new ArrayList<>();

        final List<Configuration> configurations = getConfigurationsUseCase
                .execute(input.getLogisticCenterId());

        unitsByDateOutAndDate.forEach((dateOut, unitsByDate) -> {

            if (unitsByDate.values().stream().mapToInt(Integer::intValue).sum() == 0) {
                return;
            }

            int nextBacklog = 0;
            int remainingQuantity = 0;
            boolean isDeferred = false;
            ZonedDateTime projectedDate = null;

            for (final ZonedDateTime time : unitsByDate.keySet()) {
                final int capacity = capacityByDate.getOrDefault(time, 0);
                final int unitsToProcess = unitsByDate.getOrDefault(time, 0);
                int unitsBeingProcessed = min(nextBacklog + unitsToProcess, capacity);
                final int currentBacklog = nextBacklog;

                nextBacklog += unitsToProcess - unitsBeingProcessed;

                if (unitsToProcess != 0) {
                    projectedDate = null;
                }
                // update projectedDate when all units were processed
                if (nextBacklog == 0 && currentBacklog + unitsToProcess != 0) {
                    projectedDate = calculateProjectedDate(time, capacity,
                            unitsBeingProcessed);
                }

                capacityByDate.put(time, capacity - unitsBeingProcessed);

                if (CPT == input.getProjectionType() && dateOut.truncatedTo(HOURS).isEqual(time)) {
                    final int minutes = (int) MINUTES.between(time, dateOut);
                    unitsBeingProcessed = minutes * unitsBeingProcessed / HOUR_IN_MINUTES;
                    remainingQuantity = currentBacklog - unitsBeingProcessed;
                    isDeferred = getCptsByDeferralStatus(input.getPlanningUnits())
                            .getOrDefault(dateOut, Boolean.FALSE);

                    // no left units to process
                    if (nextBacklog == 0) {
                        break;
                    }
                }

                // TODO: Remover la lógica de proyección de CAP 5 a otro lado
                if (DEFERRAL == input.getProjectionType()) {
                    final Configuration cycleTimeConfig = getCycleTimeUseCase.execute(
                            GetCycleTimeInput.builder()
                                    .cptDate(dateOut)
                                    .configurations(configurations)
                                    .build());

                    final ZonedDateTime cutOff =  dateOut.minusMinutes(cycleTimeConfig.getValue());

                    if (cutOff.truncatedTo(HOURS).isEqual(time)) {
                        final int minutes = (int) MINUTES.between(time, cutOff);
                        unitsBeingProcessed = minutes * unitsBeingProcessed / HOUR_IN_MINUTES;
                        remainingQuantity = currentBacklog - unitsBeingProcessed;
                        isDeferred = remainingQuantity > 0;

                        // no left units to process
                        if (nextBacklog == 0) {
                            break;
                        }
                    }
                }
            }

            cptProjectionOutputs.add(
                    CptProjectionOutput.builder()
                            .date(dateOut)
                            .projectedEndDate(projectedDate)
                            .remainingQuantity(remainingQuantity)
                            .isDeferred(isDeferred)
                            .build());
        });

        return cptProjectionOutputs;
    }

    private Map<ZonedDateTime, Boolean> getCptsByDeferralStatus(
            final List<GetPlanningDistributionOutput> planningUnits) {
        final Map<ZonedDateTime, List<GetPlanningDistributionOutput>> map = planningUnits.stream()
                .collect(groupingBy(GetPlanningDistributionOutput::getDateOut));

        return map.entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .allMatch(GetPlanningDistributionOutput::isDeferred)
                ));
    }

    private Map<ZonedDateTime, Map<ZonedDateTime, Integer>> getUnitsByDateOutAndDate(
            final CptProjectionInput input) {

        final Map<ZonedDateTime, Map<ZonedDateTime, Integer>> planningUnitsByDateInByDateOut =
                getPlanning(input.getPlanningUnits());

        final Map<ZonedDateTime, Integer> currentUnitsBacklogByDateOut = input.getBacklog() == null
                ? emptyMap()
                : input.getBacklog().stream().collect(toMap(
                        backlog -> backlog.getDate().withFixedOffsetZone(),
                Backlog::getQuantity, Integer::sum));

        final ZonedDateTime dateFrom = ignoreMinutes(input.getDateFrom());
        final TreeSet<ZonedDateTime> datesOut =
                new TreeSet(planningUnitsByDateInByDateOut.keySet());
        datesOut.addAll(currentUnitsBacklogByDateOut.keySet());

        final Map<ZonedDateTime, Map<ZonedDateTime, Integer>> unitsByDateOutAndDate =
                new TreeMap<>();

        datesOut.forEach(dateOut -> {
            final Map<ZonedDateTime, Integer> unitsByDate = new TreeMap<>();
            final Integer backlogQty = currentUnitsBacklogByDateOut.getOrDefault(dateOut, 0);

            unitsByDate.put(dateFrom, calculateExactBacklog(backlogQty,
                    planningUnitsByDateInByDateOut.getOrDefault(dateOut, emptyMap())
                            .getOrDefault(dateFrom, 0)));

            iterate(dateFrom.plusHours(1), date -> date.plusHours(1))
                    .limit(HOURS.between(dateFrom, input.getDateTo()))
                    .forEach(dateTime -> {
                        final Integer planningQty = planningUnitsByDateInByDateOut
                                .getOrDefault(dateOut, emptyMap()).getOrDefault(dateTime, 0);

                        unitsByDate.put(dateTime, planningQty);
                    });

            unitsByDateOutAndDate.put(dateOut, unitsByDate);
        });

        return unitsByDateOutAndDate;
    }

    private Map<ZonedDateTime, Map<ZonedDateTime, Integer>> getPlanning(
            final List<GetPlanningDistributionOutput> planningUnits) {

        return planningUnits.stream()
                .collect(groupingBy(
                        GetPlanningDistributionOutput::getDateOut,
                        toMap(
                                o -> ignoreMinutes(o.getDateIn().plusHours(1)),
                                o -> (int) o.getTotal(),
                                Integer::sum)));
        // Se agrega una hora para compensar que las ventas se graban con 0 minutos.
    }

    private ZonedDateTime calculateProjectedDate(final ZonedDateTime date,
                                                 final int capacity,
                                                 final int processedUnits) {

        if (capacity == 0 && processedUnits == 0) {
            return null;
        }

        return date.plusMinutes((processedUnits * HOUR_IN_MINUTES) / capacity);
    }

    private int calculateExactBacklog(final int backlogQty, final int planningQty) {
        final int remainingMinutes = HOUR_IN_MINUTES - LocalTime.now().getMinute();
        return backlogQty + (remainingMinutes * planningQty / HOUR_IN_MINUTES);
    }

    private void adaptMinutesFirstCapacity(final Map<ZonedDateTime, Integer> capacity) {
        final ZonedDateTime currentDate = ZonedDateTime.now();
        final int remainingMinutes = HOUR_IN_MINUTES - currentDate.getMinute();
        capacity.keySet().forEach(date -> {
            if (currentDate.truncatedTo(HOURS).isEqual(date)) {
                capacity.put(date, capacity.get(date) * remainingMinutes / HOUR_IN_MINUTES);
            }
        });
    }

}
