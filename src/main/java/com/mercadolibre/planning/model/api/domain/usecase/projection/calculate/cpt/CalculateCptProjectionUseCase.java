package com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt;

import static com.mercadolibre.planning.model.api.util.DateUtils.ignoreMinutes;
import static java.lang.Math.min;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.iterate;

import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedUnits;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings({"PMD.NullAssignment", "PMD.AvoidDeeplyNestedIfStmts", "PMD.NPathComplexity", "PMD.UselessParentheses"})
public final class CalculateCptProjectionUseCase {

  private static final int HOUR_IN_MINUTES = 60;

  private CalculateCptProjectionUseCase() { }

  public static List<CptCalculationOutput> execute(final SlaProjectionInput input) {
    final Map<ZonedDateTime, BacklogDetail> unitsByDateOutAndDate = getUnitsByDateOutAndDate(input);

    final Map<ZonedDateTime, Integer> capacity = input.getCapacity();

    adaptMinutesFirstCapacity(capacity, input.getCurrentDate());

    return project(input, capacity, unitsByDateOutAndDate);
  }
  // TODO: Refactor of nested if statements

  private static List<CptCalculationOutput> project(final SlaProjectionInput input,
                                                    final Map<ZonedDateTime, Integer> capacityByDate,
                                                    final Map<ZonedDateTime, BacklogDetail> unitsByDateOutAndDate) {

    final List<CptCalculationOutput> cptCalculationOutputs = new ArrayList<>();
    final Map<ZonedDateTime, Integer> originalCapacityByDate = new HashMap<>(capacityByDate);
    final Map<ZonedDateTime, Integer> projectionEndMinutes = new HashMap<>();

    // Iteration by dateOut (cpt)
    input.getSlaByWarehouse().forEach(sla -> {

      final BacklogDetail unitsByDate = unitsByDateOutAndDate.get(sla.getDate().withFixedOffsetZone());
      final List<CptCalculationDetailOutput> calculationDetails = new ArrayList<>();

      int nextBacklog = 0;
      int remainingQuantity = 0;
      ZonedDateTime projectedDate = input.getDateFrom();

      final boolean cptFound = unitsByDate != null;

      if (cptFound) {

        // Iteration by operation hour
        for (final ZonedDateTime operationHour : unitsByDate.getTotalBacklogByOperationHour().keySet()) {

          final int capacity = capacityByDate.getOrDefault(operationHour, 0);
          final int unitsToProcess = unitsByDate.getTotalBacklogByOperationHour().getOrDefault(operationHour, 0);
          int unitsBeingProcessed = min(nextBacklog + unitsToProcess, capacity);
          final int currentBacklog = nextBacklog;

          nextBacklog += unitsToProcess - unitsBeingProcessed;

          if (unitsToProcess != 0) {
            projectedDate = null;
          }
          // update projectedDate when all units were processed
          if (nextBacklog == 0 && currentBacklog + unitsToProcess != 0) {
            final Integer shift = projectionEndMinutes.getOrDefault(operationHour, 0);
            final Integer currentHourCapacity = originalCapacityByDate.get(operationHour);

            projectedDate = calculateProjectedDate(
                operationHour,
                currentHourCapacity,
                unitsBeingProcessed,
                shift);

            if (projectedDate != null && projectedDate.getMinute() != 0) {
              projectionEndMinutes.put(operationHour, projectedDate.getMinute() - operationHour.getMinute());
            }
          }

          capacityByDate.put(operationHour, capacity - unitsBeingProcessed);

          calculationDetails.add(new CptCalculationDetailOutput(operationHour, unitsBeingProcessed, currentBacklog));

          if (sla.getDate().truncatedTo(HOURS).isEqual(operationHour)) {
            final int minutes = (int) MINUTES.between(operationHour, sla.getDate());
            unitsBeingProcessed = minutes * unitsBeingProcessed / HOUR_IN_MINUTES;
            remainingQuantity = currentBacklog - unitsBeingProcessed;

            // no left units to process
            if (nextBacklog == 0) {
              break;
            }
          }
        }
      }

      cptCalculationOutputs.add(new CptCalculationOutput(
          sla.getDate().withFixedOffsetZone(),
          projectedDate,
          remainingQuantity,
          cptFound ? unitsByDate.getTotalCurrentBacklog() : 0,
          cptFound ? unitsByDate.getTotalPlannedBacklog() : 0,
          calculationDetails));
    });

    return cptCalculationOutputs;
  }

  private static SortedMap<ZonedDateTime, BacklogDetail> getUnitsByDateOutAndDate(final SlaProjectionInput input) {

    final Map<ZonedDateTime, Map<ZonedDateTime, Integer>> expectedUnitsByDateInByDateOut = getExpectedUnits(input.getPlannedUnits());

    final Map<ZonedDateTime, Integer> currentUnitsBacklogByDateOut = input.getBacklog() == null
        ? emptyMap()
        : input.getBacklog().stream().collect(toMap(
        backlog -> backlog.getDate().withFixedOffsetZone(),
        Backlog::getQuantity, Integer::sum));

    final ZonedDateTime firstHour = ignoreMinutes(input.getDateFrom());

    final TreeSet<ZonedDateTime> datesOut = new TreeSet<>(expectedUnitsByDateInByDateOut.keySet());
    datesOut.addAll(currentUnitsBacklogByDateOut.keySet());

    final SortedMap<ZonedDateTime, BacklogDetail> unitsByDateOutAndDate = new TreeMap<>();

    datesOut.forEach(dateOut -> {
      final Map<ZonedDateTime, Integer> unitsByDate = new TreeMap<>();
      final Integer totalCurrentBacklog = currentUnitsBacklogByDateOut.getOrDefault(dateOut, 0);
      final Map<ZonedDateTime, Integer> expectedUnitsByDateOut = expectedUnitsByDateInByDateOut.getOrDefault(dateOut, emptyMap());

      // Process first hours
      unitsByDate.put(input.getCurrentDate(), calculateProportionalQuantityToFirstDate(
          totalCurrentBacklog,
          expectedUnitsByDateOut.getOrDefault(firstHour, 0),
          input.getCurrentDate().getMinute()));

      // Process next hours (excluded first hour)
      iterate(firstHour.plusHours(1), date -> date.plusHours(1))
          .limit(HOURS.between(firstHour, input.getDateTo()))
          .forEach(dateTime -> {
            final Integer expectedQty =
                expectedUnitsByDateOut.getOrDefault(dateTime, 0);

            unitsByDate.put(dateTime, expectedQty);
          });

      final int totalPlannedBacklog = unitsByDate.values().stream().reduce(0, Integer::sum) - totalCurrentBacklog;
      unitsByDateOutAndDate.put(dateOut, new BacklogDetail(totalCurrentBacklog, totalPlannedBacklog, unitsByDate));
    });

    return unitsByDateOutAndDate;
  }

  private static Map<ZonedDateTime, Map<ZonedDateTime, Integer>> getExpectedUnits(final List<PlannedUnits> plannedUnits) {
    return plannedUnits.stream()
        .collect(groupingBy(
            PlannedUnits::getDateOut,
            toMap(
                o -> ignoreMinutes(o.getDateIn().plusHours(1)),
                o -> (int) o.getTotal(),
                Integer::sum))
        );
    // Se agrega una hora para compensar que las ventas se graban con 0 minutos.
  }

  private static ZonedDateTime calculateProjectedDate(final ZonedDateTime date,
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

  private static int calculateProportionalQuantityToFirstDate(final int backlogQty,
                                                              final int expectedQty,
                                                              final int nowMinutes) {
    final int remainingMinutes = HOUR_IN_MINUTES - nowMinutes;
    return backlogQty + (remainingMinutes * expectedQty / HOUR_IN_MINUTES);
  }

  private static void adaptMinutesFirstCapacity(final Map<ZonedDateTime, Integer> capacity,
                                                final ZonedDateTime currentDate) {

    final int currentDateCapacity = capacity.getOrDefault(ignoreMinutes(currentDate), 0);
    capacity.remove(ignoreMinutes(currentDate));

    final int remainingMinutes = HOUR_IN_MINUTES - currentDate.getMinute();
    capacity.put(currentDate, currentDateCapacity * remainingMinutes / HOUR_IN_MINUTES);
  }
}
