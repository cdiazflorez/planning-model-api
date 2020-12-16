package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.strategy.BacklogProjectionStrategy;
import com.mercadolibre.planning.model.api.util.DateUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.api.util.DateUtils.nextHour;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingLong;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.iterate;

@Service
@AllArgsConstructor
public class CalculateBacklogProjectionUseCase {

    private static final int HOUR_IN_MINUTES = 60;

    private final BacklogProjectionStrategy backlogProjectionStrategy;

    public List<BacklogProjectionOutput> execute(final BacklogProjectionInput input) {
        final List<BacklogProjectionOutput> outputs = new ArrayList<>(estimateSize(input));

        for (final ProcessName processName : input.getProcessNames()) {
            final ProcessParams processParams = backlogProjectionStrategy.getBy(processName)
                    .orElseThrow() //TODO: Add Exception
                    .execute(input);

            if (processParams.getProcessName().isConsiderPreviousBacklog()) {
                for (final ProcessName previousProcess : processName.getPreviousProcesses()) {
                    final List<BacklogProjectionOutputValue> previousBacklogs = outputs.stream()
                            .filter(o -> o.getProcessName() == previousProcess)
                            .findFirst()
                            .get()
                            .getValues();

                    processParams.setPreviousBacklogsByDate(adaptToMap(previousBacklogs));
                }
            }

            outputs.add(calculateProjectionOutput(input, processParams));
        }

        return outputs;
    }

    private int estimateSize(final BacklogProjectionInput projectionInput) {
        return projectionInput.getProcessNames().size()
                * (int) HOURS.between(projectionInput.getDateFrom(), projectionInput.getDateTo());
    }

    private Map<ZonedDateTime, Long> adaptToMap(final List<BacklogProjectionOutputValue> values) {
        return values.stream()
                .collect(groupingBy(BacklogProjectionOutputValue::getDate,
                        summingLong(BacklogProjectionOutputValue::getQuantity)));
    }

    private BacklogProjectionOutput calculateProjectionOutput(final BacklogProjectionInput input,
                                                              final ProcessParams processParams) {

        final ZonedDateTime dateFrom = input.getDateFrom().withFixedOffsetZone();
        final List<ZonedDateTime> dates = getDatesInRange(dateFrom, input.getDateTo());
        long processBacklog = processParams.getCurrentBacklog();
        final List<BacklogProjectionOutputValue> processValues = new ArrayList<>(dates.size());

        for (final ZonedDateTime date : dates) {
            final int capacity = getPreviousProcessCapacity(date, dateFrom, processParams);
            final long planningUnits = getPlanningUnitsQuantity(date, dateFrom, processParams);

            final long quantity = max(processBacklog + planningUnits - capacity, 0);

            processValues.add(BacklogProjectionOutputValue.builder()
                    .quantity(quantity)
                    .date(date)
                    .build());

            processBacklog = quantity;
        }

        return new BacklogProjectionOutput(processParams.getProcessName(), processValues, FORECAST);
    }

    private long getPlanningUnitsQuantity(final ZonedDateTime date,
                                          final ZonedDateTime dateFrom,
                                          final ProcessParams processParams) {
        final boolean isFirstDate = nextHour(dateFrom).equals(date);
        final Map<ZonedDateTime, Long> planningUnitsByDate = getDefinitiveUnits(processParams);

        final ZonedDateTime afterDate = isFirstDate
                ? dateFrom.truncatedTo(HOURS).minusMinutes(1)
                : date.withFixedOffsetZone().minusHours(1).minusMinutes(1);

        final ZonedDateTime beforeDate = isFirstDate
                ? nextHour(dateFrom)
                : date.withFixedOffsetZone().minusMinutes(1);

        final long fullHourUnits =  planningUnitsByDate.entrySet().stream()
                .filter(map -> map.getKey().isAfter(afterDate)
                        && map.getKey().isBefore(beforeDate))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue))
                .values().stream().reduce(0L, Long::sum);

        return isFirstDate
                ? calculatePercentage(dateFrom, (int) fullHourUnits)
                : fullHourUnits;
    }

    // We need to add this logic only for Picking process
    private Map<ZonedDateTime, Long> getDefinitiveUnits(final ProcessParams processParams) {
        final Map<ZonedDateTime, Long> planningUnitsByDate = processParams.getPlanningUnitsByDate();

        if (!processParams.getProcessName().isConsiderPreviousBacklog()) {
            return planningUnitsByDate;
        }

        final Map<ZonedDateTime, Long> incomingUnitsByDate = new HashMap<>();
        for (final Map.Entry<ZonedDateTime, Long> unitByDate : planningUnitsByDate.entrySet()) {
            final ZonedDateTime date = unitByDate.getKey();
            if (processParams.getPreviousBacklogsByDate().get(date) != null) {
                incomingUnitsByDate.put(date,
                        min(planningUnitsByDate.get(date),
                                processParams.getPreviousBacklogsByDate().get(date)));
            }
        }

        return incomingUnitsByDate;
    }

    private int getPreviousProcessCapacity(final ZonedDateTime date,
                                           final ZonedDateTime dateFrom,
                                           final ProcessParams processParams) {
        final boolean isFirstDate = nextHour(dateFrom).equals(date);
        final Map<ZonedDateTime, Integer> capacityByDate = processParams.getCapacityByDate();

        if (isFirstDate) {
            // If it is the first process, it has no previous process capacity
            return processParams.getProcessName().getPreviousProcesses() == null
                    ? 0
                    : calculatePercentage(
                            dateFrom, capacityByDate.getOrDefault(dateFrom.truncatedTo(HOURS), 0));
        } else {
            return capacityByDate.getOrDefault(date.minusHours(1), 0);
        }
    }

    private List<ZonedDateTime> getDatesInRange(final ZonedDateTime dateFrom,
                                                final ZonedDateTime dateTo) {
        final List<ZonedDateTime> dates = new ArrayList<>();
        iterate(nextHour(dateFrom).withFixedOffsetZone(), DateUtils::nextHour)
                .limit(HOURS.between(nextHour(dateFrom).withFixedOffsetZone(),
                        dateTo.withFixedOffsetZone().plusHours(1)))
                .forEach(dates::add);
        return dates;
    }

    private int calculatePercentage(final ZonedDateTime dateFrom,
                                    final int completeHourQuantity) {
        final int minutes = dateFrom.withFixedOffsetZone().getMinute();
        return minutes <= 30
                ? completeHourQuantity * minutes / HOUR_IN_MINUTES
                : completeHourQuantity * (HOUR_IN_MINUTES - minutes) / HOUR_IN_MINUTES;
    }
}
