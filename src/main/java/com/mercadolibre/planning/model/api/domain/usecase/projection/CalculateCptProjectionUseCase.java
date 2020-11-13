package com.mercadolibre.planning.model.api.domain.usecase.projection;

import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.web.controller.request.ProjectionType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.mercadolibre.planning.model.api.web.controller.request.ProjectionType.CPT;
import static java.lang.Math.min;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.iterate;

@Service
@AllArgsConstructor
public class CalculateCptProjectionUseCase implements CalculateProjectionUseCase {

    private static final int HOUR_IN_MINUTES = 60;

    @Override
    public boolean supportsProjectionType(final ProjectionType projectionType) {
        return CPT == projectionType;
    }

    @Override
    public List<ProjectionOutput> execute(final ProjectionInput input) {
        final Map<ZonedDateTime, Integer> capacityByDate = getCapacity(input.getThroughput());
        final Map<ZonedDateTime, Map<ZonedDateTime, Integer>> unitsByDateOutAndDate =
                getUnitsByDateOutAndDate(input);

        return project(capacityByDate, unitsByDateOutAndDate);
    }

    @SuppressWarnings("PMD.NullAssignment")
    private List<ProjectionOutput> project(
            final Map<ZonedDateTime, Integer> capacityByDate,
            final Map<ZonedDateTime, Map<ZonedDateTime, Integer>> unitsByDateOutAndDate) {

        final List<ProjectionOutput> projectionOutputs = new ArrayList<>();
        unitsByDateOutAndDate.forEach((dateOut, unitsByDate) -> {

            if (unitsByDate.values().stream().mapToInt(Integer::intValue).sum() == 0) {
                return;
            }
            int nextBacklog = 0;
            int remainingQuantity = 0;
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

                if (dateOut.truncatedTo(HOURS).isEqual(time)) {
                    final int minutes = (int) MINUTES.between(time, dateOut);
                    unitsBeingProcessed = minutes * unitsBeingProcessed / HOUR_IN_MINUTES;
                    remainingQuantity = currentBacklog - unitsBeingProcessed;

                    // no left units to process
                    if (nextBacklog == 0) {
                        break;
                    }
                }
            }
            projectionOutputs.add(new ProjectionOutput(dateOut, projectedDate, remainingQuantity));
        });

        return projectionOutputs;
    }

    private Map<ZonedDateTime, Integer> getCapacity(
            final List<EntityOutput> throughput) {

        return throughput.stream().collect(toMap(
                EntityOutput::getDate,
                entityOutput -> (int) entityOutput.getValue(),
                Math::min));
    }

    private Map<ZonedDateTime, Map<ZonedDateTime, Integer>> getUnitsByDateOutAndDate(
            final ProjectionInput input) {

        final Map<ZonedDateTime, Map<ZonedDateTime, Integer>> planningUnitsByDateInByDateOut =
                getPlanning(input.getPlanningUnits());

        final Map<ZonedDateTime, Integer> currentUnitsBacklogByDateOut = input.getBacklog() == null
                ? emptyMap()
                : input.getBacklog().stream().collect(toMap(
                        backlog -> backlog.getDate().withFixedOffsetZone(),
                        Backlog::getQuantity, Integer::sum));

        final ZonedDateTime dateFrom = ignoreMinutes(input.getDateFrom());
        final Map<ZonedDateTime, Map<ZonedDateTime, Integer>> unitsByDateOutAndDate =
                new TreeMap<>();

        planningUnitsByDateInByDateOut.keySet().forEach(dateOut -> {
            final Map<ZonedDateTime, Integer> unitsByDate = new TreeMap<>();
            final Integer backlogQty = currentUnitsBacklogByDateOut.getOrDefault(dateOut, 0);

            unitsByDate.put(dateFrom, calculateExactBacklog(backlogQty,
                    planningUnitsByDateInByDateOut.get(dateOut).getOrDefault(dateFrom, 0)));

            iterate(dateFrom.plusHours(1), date -> date.plusHours(1))
                    .limit(HOURS.between(dateFrom, input.getDateTo()))
                    .forEach(dateTime -> {
                        final Integer planningQty = planningUnitsByDateInByDateOut.get(dateOut)
                                .getOrDefault(dateTime, 0);

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
                                o -> ignoreMinutes(o.getDateIn()),
                                o -> (int) o.getTotal(),
                                Integer::sum)));
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

    private ZonedDateTime ignoreMinutes(final ZonedDateTime dateTime) {
        return dateTime.truncatedTo(HOURS).withFixedOffsetZone();
    }
}
