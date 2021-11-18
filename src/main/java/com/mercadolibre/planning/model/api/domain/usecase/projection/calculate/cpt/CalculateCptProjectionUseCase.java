package com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt;

import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.mercadolibre.planning.model.api.util.DateUtils.ignoreMinutes;
import static java.lang.Math.min;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.iterate;

@Slf4j
@Service
@AllArgsConstructor
@SuppressWarnings({"PMD.NullAssignment", "PMD.AvoidDeeplyNestedIfStmts",
        "PMD.NPathComplexity", "PMD.UselessParentheses"})
public class CalculateCptProjectionUseCase {

    private static final int HOUR_IN_MINUTES = 60;

    public List<CptCalculationOutput> execute(final CptProjectionInput input) {
        final Map<ZonedDateTime, Map<ZonedDateTime, Integer>> unitsByDateOutAndDate =
                getUnitsByDateOutAndDate(input);

        final Map<ZonedDateTime, Integer> capacity = input.getCapacity();
        adaptMinutesFirstCapacity(capacity, input.getCurrentDate());

        return project(input, capacity, unitsByDateOutAndDate);
    }

    // TODO: Refactor of nested if statements

    private List<CptCalculationOutput> project(
            final CptProjectionInput input,
            final Map<ZonedDateTime, Integer> capacityByDate,
            final Map<ZonedDateTime, Map<ZonedDateTime, Integer>> unitsByDateOutAndDate) {

        final List<CptCalculationOutput> cptCalculationOutputs = new ArrayList<>();
        final Map<ZonedDateTime, Integer> originalCapacityByDate = new HashMap<>(capacityByDate);
        final Map<ZonedDateTime, Integer> projectionEndMinutes = new HashMap<>();

        input.getCptByWarehouse().forEach(item -> {

            final Map<ZonedDateTime, Integer> unitsByDate =
                    unitsByDateOutAndDate.get(item.getDate().withFixedOffsetZone());

            int nextBacklog = 0;
            int remainingQuantity = 0;
            ZonedDateTime projectedDate = input.getDateFrom();

            final boolean cptFound = unitsByDate != null;

            if (cptFound) {

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
                        final Integer shift = projectionEndMinutes.getOrDefault(time, 0);
                        final Integer currentHourCapacity = originalCapacityByDate.get(time);

                        projectedDate = calculateProjectedDate(
                                time,
                                currentHourCapacity,
                                unitsBeingProcessed,
                                shift);

                        if (projectedDate != null && projectedDate.getMinute() != 0) {
                            projectionEndMinutes.put(time,
                                    projectedDate.getMinute() - time.getMinute());
                        }
                    }

                    capacityByDate.put(time, capacity - unitsBeingProcessed);

                    if (item.getDate().truncatedTo(HOURS).isEqual(time)) {
                        final int minutes = (int) MINUTES.between(time, item.getDate());
                        unitsBeingProcessed = minutes * unitsBeingProcessed / HOUR_IN_MINUTES;
                        remainingQuantity = currentBacklog - unitsBeingProcessed;

                        // no left units to process
                        if (nextBacklog == 0) {
                            break;
                        }
                    }
                }
            }

            cptCalculationOutputs.add(
                    new CptCalculationOutput(item.getDate().withFixedOffsetZone(),
                            projectedDate, remainingQuantity));

        });

        return cptCalculationOutputs;
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

            unitsByDate.put(input.getCurrentDate(),
                    calculateExactBacklog(
                            backlogQty,
                            planningUnitsByDateInByDateOut.getOrDefault(dateOut, emptyMap())
                                    .getOrDefault(dateFrom, 0),
                            input.getCurrentDate().getMinute()));

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
                                                 final int processedUnits,
                                                 final Integer shiftMinutes) {

        if (capacity == 0) {
            return null;
        }

        final int baseMinutes = HOUR_IN_MINUTES - date.getMinute();
        final int minutes = (processedUnits * baseMinutes / capacity) + shiftMinutes;
        return date.plusMinutes(minutes);
    }

    private int calculateExactBacklog(final int backlogQty,
                                      final int planningQty,
                                      final int nowMinutes) {
        final int remainingMinutes = HOUR_IN_MINUTES - nowMinutes;
        return backlogQty + (remainingMinutes * planningQty / HOUR_IN_MINUTES);
    }

    private void adaptMinutesFirstCapacity(final Map<ZonedDateTime, Integer> capacity,
                                           final ZonedDateTime currentDate) {

        final int currentDateCapacity = capacity.get(ignoreMinutes(currentDate));
        capacity.remove(ignoreMinutes(currentDate));

        final int remainingMinutes = HOUR_IN_MINUTES - currentDate.getMinute();
        capacity.put(currentDate, currentDateCapacity * remainingMinutes / HOUR_IN_MINUTES);
    }
}
