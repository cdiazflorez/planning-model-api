package com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt;

import static com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityInput.fromEntityOutputs;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.sla.ProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedBacklogService;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedUnits;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.GetCapacityPerHourService;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeService;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.QueueProjectionCalculator.Assistant;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.QueueProjectionCalculator.Queue;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetSlaProjectionInput;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueProjectionService {

  private static final long DEFAULT_CYCLE_TIME_IN_MINUTES = 40;

  private final GetThroughputUseCase getThroughputUseCase;

  private final GetCapacityPerHourService getCapacityPerHourService;

  private final PlannedBacklogService plannedBacklogService;

  private final GetCycleTimeService getCycleTimeService;

  private static Set<Instant> getAllDateOuts(
      final List<QuantityByDate> backlog,
      final NavigableMap<Instant, NavigableMap<Instant, Long>> upstreamQuantityByDateOutByDateIn
  ) {

    final var backlogDateOuts = backlog.stream().map(QuantityByDate::getDate).map(ZonedDateTime::toInstant);
    final var upstreamDateOuts = upstreamQuantityByDateOutByDateIn.values().stream()
        .flatMap(upstreamQuantityByDateOUt -> upstreamQuantityByDateOUt.keySet().stream());
    return Stream.concat(backlogDateOuts, upstreamDateOuts).collect(Collectors.toSet());
  }

  public List<CptProjectionOutput> calculateCptProjection(final GetSlaProjectionInput input) {
    final var startingDate = input.getDateFrom().toInstant();
    final var endingDate = input.getDateTo().toInstant();

    final var initialBacklog = (input.getBacklog() == null)
        ? List.<QuantityByDate>of()
        : input.getBacklog().stream().filter(entry -> entry.getQuantity() > 0).collect(Collectors.toList());
    final NavigableMap<Instant, Integer> capacity = getCapacity(input);

    // Forecasted units grouped by date in and date out
    final var upstreamQuantityByDateOutByDateIn = getForecastedUpstream(input);
    final Set<Instant> allDateOuts = getAllDateOuts(initialBacklog, upstreamQuantityByDateOutByDateIn);
    final Map<Instant, Long> cycleTimes = getCycleTimes(input.getWarehouseId(), allDateOuts);

    // Gets each sla with its cut off
    final Function<Instant, Instant> dateOutToCutoffMapper =
        dateOut -> dateOut.minus(cycleTimes.getOrDefault(dateOut, DEFAULT_CYCLE_TIME_IN_MINUTES), ChronoUnit.MINUTES);

    final var initialQueue = getInitialQueue(initialBacklog, dateOutToCutoffMapper);

    final var allCutoffs = allDateOuts.stream()
        .map(dateOutToCutoffMapper)
        .filter(cutoff -> !startingDate.isAfter(cutoff) && !cutoff.isAfter(endingDate))
        .collect(Collectors.toSet());

    // In order to simplify the {@link Assistant} implementation, not only the cutoff instants are included, but also all the instants
    // where the capacity or the upstream output are discontinuous.
    final var inflectionPoints = getInflectionInstants(
        upstreamQuantityByDateOutByDateIn,
        capacity,
        allCutoffs,
        startingDate,
        endingDate
    );

    final var assistant = new AssistantImpl(upstreamQuantityByDateOutByDateIn, capacity, dateOutToCutoffMapper);

    final var projectionLog = QueueProjectionCalculator.calculate(
        startingDate,
        initialQueue,
        inflectionPoints,
        assistant
    );

    final var queueAtLastInflectionPoint = projectionLog.recordByStepEndingDate.lastEntry().getValue().queueAtEndOfStep;
    final var allProjectionRecordsReversed = projectionLog.recordByStepEndingDate.descendingMap().values();

    return allDateOuts.stream()
        .map(dateOut -> new Sla(dateOut, dateOutToCutoffMapper.apply(dateOut)))
        .map(sla -> {
          final var recordAtCutoff = projectionLog.recordByStepEndingDate.get(sla.cutoff);
          final var recordAtStartingDate = projectionLog.recordByStepEndingDate.get(startingDate);

          final var expiredQuantity = !sla.cutoff.isAfter(startingDate)
              ? recordAtStartingDate.queueAtEndOfStep.getQuantityByDiscriminator().getOrDefault(sla.cutoff, 0L)
              : recordAtCutoff.queueAtEndOfStep.getQuantityByDiscriminator().getOrDefault(sla.cutoff, 0L);
          final var isExhausted = queueAtLastInflectionPoint.getQuantityByDiscriminator().getOrDefault(sla.cutoff, 0L) == 0;

          // Note that when the initial backlog and the forecasted backlog are both zero the projected end date is set to the
          // starting time.
          // Note that when the backlog is not exhausted before the end of the projection scope the projected end date is set
          // to null.
          final var projectedEndDate = isExhausted
              ? allProjectionRecordsReversed.stream()
              .map(projectionRecord -> projectionRecord.exhaustionInstantByDiscriminator.get(sla.cutoff))
              .filter(Objects::nonNull)
              .map(i -> ZonedDateTime.ofInstant(i, ZoneOffset.UTC))
              .findFirst().orElse(input.getDateFrom())
              : null;
          return new CptProjectionOutput(
              ZonedDateTime.ofInstant(sla.dateOut, ZoneOffset.UTC),
              projectedEndDate,
              expiredQuantity.intValue(),
              new ProcessingTime(cycleTimes.getOrDefault(sla.dateOut, 0L), MetricUnit.MINUTES)
          );
        })
        .sorted(Comparator.comparing(CptProjectionOutput::getDate))
        .collect(Collectors.toList());
  }

  private NavigableMap<Instant, Integer> getCapacity(final GetSlaProjectionInput input) {
    final List<EntityOutput> throughput = getThroughputUseCase.execute(
        GetEntityInput
            .builder()
            .warehouseId(input.getWarehouseId())
            .workflow(input.getWorkflow())
            .dateFrom(input.getDateFrom().truncatedTo(ChronoUnit.HOURS))
            .dateTo(input.getDateTo())
            .processName(input.getProcessName())
            .simulations(input.getSimulations())
            .source(input.getSource())
            .build()
    );

    return getCapacityPerHourService
        .execute(input.getWorkflow(), fromEntityOutputs(throughput))
        .stream()
        .collect(toMap(
            co -> co.getDate().toInstant(),
            capacityOutput -> (int) capacityOutput.getValue(),
            Integer::sum,
            TreeMap::new
        ));
  }

  private NavigableMap<Instant, NavigableMap<Instant, Long>> getForecastedUpstream(final GetSlaProjectionInput input) {
    final List<PlannedUnits> upstreamForecast = plannedBacklogService.getExpectedBacklog(
        input.getWarehouseId(),
        input.getWorkflow(),
        input.getDateFrom().truncatedTo(ChronoUnit.HOURS),
        input.getDateTo(),
        input.getViewDate(),
        input.isApplyDeviation()
    );
    return upstreamForecast.stream()
        .filter(entry -> entry.getTotal() > 0)
        .collect(groupingBy(
            pu -> pu.getDateIn().toInstant(),
            TreeMap::new,
            toMap(
                pu -> pu.getDateOut().toInstant(),
                PlannedUnits::getTotal,
                Long::sum,
                TreeMap::new
            )
        ));
  }

  private Map<Instant, Long> getCycleTimes(String warehouseId, final Set<Instant> allDateOuts) {
    final var allDateOutsZdt = allDateOuts.stream().map(i -> ZonedDateTime.ofInstant(i, ZoneOffset.UTC)).collect(Collectors.toList());
    final var cycleTimes = getCycleTimeService.execute(new GetCycleTimeInput(warehouseId, allDateOutsZdt));
    return cycleTimes.entrySet().stream().collect(toMap(
        entry -> entry.getKey().toInstant(),
        Map.Entry::getValue
    ));
  }

  private Queue<Instant> getInitialQueue(final List<QuantityByDate> quantityByDateOut, final Function<Instant, Instant> cutoffMapper) {
    final var quantityByCutoff = quantityByDateOut.stream().collect(toMap(
        quantityAtDateOut -> cutoffMapper.apply(quantityAtDateOut.getDate().toInstant()),
        quantityAtDateOut -> (long) quantityAtDateOut.getQuantity().intValue(),
        Long::sum,
        TreeMap::new
    ));
    return new Queue<>(quantityByCutoff);
  }

  /**
   * Gives all the instants where the {@link Queue} should be calculated, and also, in order to simplify the {@link Assistant}
   * implementation, all the instants where the processing power (capacity) or the upstream output are discontinuous.
   */
  private NavigableSet<Instant> getInflectionInstants(
      final Map<Instant, NavigableMap<Instant, Long>> upstreamQuantityByDateOutByDateIn,
      final Map<Instant, Integer> capacity,
      final Set<Instant> allCutoffs,
      final Instant fromExclusive,
      final Instant toInclusive
  ) {

    var allInputInflections = Stream.concat(
        capacity.keySet().stream(),
        Stream.concat(
            upstreamQuantityByDateOutByDateIn.keySet().stream(),
            Stream.of(toInclusive)
        )
    ).filter(date -> fromExclusive.isBefore(date) && !date.isAfter(toInclusive));
    return Stream.concat(allInputInflections, allCutoffs.stream())
        .collect(Collectors.toCollection(TreeSet::new));
  }


  @RequiredArgsConstructor
  private static class Sla {
    final Instant dateOut;

    final Instant cutoff;
  }
}
