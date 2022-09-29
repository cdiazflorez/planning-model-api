package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.BacklogProcessedUnits;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.BacklogProjection;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.BacklogProjectionOutputValue;
import com.mercadolibre.planning.model.api.util.DateUtils;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.api.util.DateUtils.nextHour;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.iterate;

@Service
@AllArgsConstructor
public class CalculateBacklogProjectionUseCase {

    private static final int HOUR_IN_MINUTES = 60;

    private static final double DEFAULT_RATIO = 0.5;

    private final BacklogProjectionStrategy projectionStrategy;

    public List<BacklogProjection> execute(final BacklogProjectionInput input) {
        final List<BacklogProcessedUnits> outputs = new ArrayList<>(estimateSize(input));

        final List<ProcessName> processes = input.getProcessNames().stream()
                .sorted()
                .collect(Collectors.toList());

        for (final ProcessName process : processes) {
            final Optional<GetBacklogProjectionParamsUseCase> useCase = projectionStrategy.getBy(process);
            if (useCase.isPresent()) {
                final ProcessParams processParams = useCase.get().execute(process, input);

                if (processParams.getProcessName().isConsiderPreviousBacklog()) {
                    final Map<ZonedDateTime, Long> processedUnits = outputs
                            .stream()
                            .filter(o -> o.getBacklogProjection().getProcessName() == process.getPreviousProcesses())
                            .findFirst()
                            .map(BacklogProcessedUnits::getProcessedUnits).orElse(Collections.emptyMap());

                    processParams.setProcessedUnitsByDate(processedUnits);
                }

                outputs.add(calculateProjectionOutput(input, processParams));
            }
        }
        return outputs
            .stream()
            .map(BacklogProcessedUnits::getBacklogProjection)
            .collect(Collectors.toList());
    }

    private int estimateSize(final BacklogProjectionInput projectionInput) {
        return projectionInput.getProcessNames().size()
                * (int) HOURS.between(projectionInput.getDateFrom(), projectionInput.getDateTo());
    }

    private BacklogProcessedUnits calculateProjectionOutput(final BacklogProjectionInput input,
                                                        final ProcessParams processParams) {

        final ZonedDateTime dateFrom = input.getDateFrom().withFixedOffsetZone();
        final List<ZonedDateTime> dates = getDatesInRange(dateFrom, input.getDateTo());
        long processBacklog = processParams.getCurrentBacklog();
        final List<BacklogProjectionOutputValue> processValues = new ArrayList<>(dates.size());

        Map<ZonedDateTime, Long> planningUnitsByDate = getDefinitiveUnits(processParams);
        final Map<ZonedDateTime, Long> processUnitsByDate = new ConcurrentHashMap<>();

        for (final ZonedDateTime date : dates) {
            final long capacity = getPreviousProcessCapacity(date, dateFrom, processParams);
            final long planningUnits = getPlanningUnitsQuantity(date, dateFrom, planningUnitsByDate);

            final long quantity = max(processBacklog + planningUnits - capacity, 0);
            final long processUnits = min(capacity, processBacklog + planningUnits);
            processUnitsByDate.put(date, processUnits);
            processValues.add(BacklogProjectionOutputValue.builder()
                    .quantity(quantity)
                    .date(date)
                    .build());

            processBacklog = quantity;
        }

        return new BacklogProcessedUnits(
            new BacklogProjection(processParams.getProcessName(), processValues, FORECAST),
            processUnitsByDate
        );
    }

    private long getPlanningUnitsQuantity(final ZonedDateTime date,
                                          final ZonedDateTime dateFrom,
                                          Map<ZonedDateTime, Long> planningUnitsByDate) {
        final boolean isFirstDate = nextHour(dateFrom).equals(date);

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
            if (processParams.getProcessedUnitsByDate().get(date) != null) {
                final Long processedUnits = processParams.getRatiosByDate() == null
                    ? processParams.getProcessedUnitsByDate().get(date)
                    : (long) (processParams.getProcessedUnitsByDate().get(date)
                        * processParams.getRatiosByDate().getOrDefault(date, DEFAULT_RATIO));
                incomingUnitsByDate.put(date, processedUnits);
            }
        }

        return incomingUnitsByDate;
    }

    private long getPreviousProcessCapacity(final ZonedDateTime date,
                                            final ZonedDateTime dateFrom,
                                            final ProcessParams processParams) {
        final boolean isFirstDate = nextHour(dateFrom).equals(date);
        final Map<ZonedDateTime, Long> capacityByDate = processParams.getCapacityByDate();

        if (isFirstDate) {
            // If it is the first process, it has no previous process capacity
            return processParams.getProcessName().getPreviousProcesses() == null
                    ? 0
                    : calculatePercentage(dateFrom, capacityByDate.getOrDefault(dateFrom.truncatedTo(HOURS), 0L));
        } else {
            return capacityByDate.getOrDefault(date.minusHours(1), 0L);
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

    private long calculatePercentage(final ZonedDateTime dateFrom,
                                    final long completeHourQuantity) {
        final int minutes = dateFrom.getMinute();
        return completeHourQuantity * (HOUR_IN_MINUTES - minutes) / HOUR_IN_MINUTES;
    }
}

