package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityInput.fromEntityOutputs;
import static com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.DeliveryPromiseProjectionUtils.getSlasToBeProjectedFromBacklogAndKnowSlas;
import static com.mercadolibre.planning.model.api.util.DateUtils.MINUTES_IN_HOUR;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedBacklogService;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedUnits;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.GetCapacityPerHourService;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeService;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get.MaxCapacityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get.MaxCapacityService;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionService;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.helper.BacklogBySlaHelper;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.BacklogBySla;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.PlannedBacklogBySla;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.ThroughputByHour;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.QuantityAtDate;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.DeliveryPromiseProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeferralProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.output.DeferralProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.sla.GetSlaByWarehouseOutboundService;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@SuppressWarnings({"PMD.ExcessiveImports", "PMD.LongVariable"})
@Slf4j
@Component
@AllArgsConstructor
public class GetDeferralProjectionUseCase {

  private static final int INTERVAL_WIDTH_MINUTES = 5;

  private static final long DEFERRAL_DAYS_TO_PROJECT = 3;

  private final MaxCapacityService maxCapacityService;

  private final GetCycleTimeService getCycleTimeService;

  private final GetSlaByWarehouseOutboundService getSlaByWarehouseOutboundService;

  private final PlannedBacklogService plannedBacklogService;

  private final GetThroughputUseCase getThroughputUseCase;

  private final GetCapacityPerHourService getCapacityPerHourService;

  public List<DeferralProjectionOutput> execute(final GetDeferralProjectionInput input) {
    final List<GetSlaByWarehouseOutput> allCptByWarehouse = getSlaByWarehouseOutboundService.execute(
        new GetSlaByWarehouseInput(
            input.getLogisticCenterId(),
            input.getSlaFrom(),
            input.getSlaTo(),
            DeliveryPromiseProjectionUtils.getCptDefaultFromBacklog(input.getBacklog()),
            input.getTimeZone()
        ));

    final List<ZonedDateTime> slas = getSlasToBeProjectedFromBacklogAndKnowSlas(input.getBacklog(), allCptByWarehouse);

    final Map<ZonedDateTime, Long> cycleTimeByCpt = getCycleTimeService.execute(new GetCycleTimeInput(input.getLogisticCenterId(), slas));

    final PlannedBacklogBySla plannedBacklogBySla = getIncomingBacklog(input);
    final ThroughputByHour throughput = getThroughput(input);

    final var start = System.nanoTime();
    final var result =
        getDeferralProjectionsByLoopingOverPossibleBacklogStates(input, allCptByWarehouse, cycleTimeByCpt, plannedBacklogBySla, throughput);
    final var end = System.nanoTime();

    log.info("total time spent on deferral projection: {}", TimeUnit.NANOSECONDS.toMillis(end - start));

    result.sort(Comparator.comparing(DeferralProjectionOutput::getSla));
    return result;
  }

  private List<DeliveryPromiseProjectionOutput> getDeferredSlas(final ZonedDateTime currentUtcDate,
                                                                final List<Backlog> backlogs,
                                                                final List<GetSlaByWarehouseOutput> allCptByWarehouse,
                                                                final Map<ZonedDateTime, Long> cycleTimeByCpt,
                                                                final Map<ZonedDateTime, Integer> maxCapacity) {

    final var modifiableMaxCaps = new HashMap<>(maxCapacity);
    final var dateFrom = currentUtcDate.truncatedTo(ChronoUnit.HOURS);
    final var dateTo = dateFrom.plusDays(DEFERRAL_DAYS_TO_PROJECT);

    final List<DeliveryPromiseProjectionOutput> projections = DeliveryPromiseCalculator.calculate(
        dateFrom,
        dateTo,
        currentUtcDate,
        backlogs,
        modifiableMaxCaps,
        allCptByWarehouse,
        cycleTimeByCpt
    );

    return projections.stream()
        .filter(DeliveryPromiseProjectionOutput::isDeferred)
        .collect(toList());
  }

  private List<Backlog> createNextState(final List<Backlog> currentState,
                                        final PlannedBacklogBySla plannedBacklogBySla,
                                        final ThroughputByHour tph,
                                        final List<Instant> projectionDates,
                                        final BacklogBySlaHelper helper) {
    final var projectedBacklog = CalculateBacklogProjectionService.project(
        projectionDates,
        plannedBacklogBySla,
        getCurrentBacklog(currentState),
        tph,
        helper
    );

    return projectedBacklog.get(0)
        .getResultingState()
        .getCarryOver()
        .getDistributions()
        .stream()
        .map(q -> new Backlog(ZonedDateTime.ofInstant(q.getDate(), ZoneOffset.UTC), q.getQuantity()))
        .collect(toList());
  }

  private PlannedBacklogBySla removeDeferredSlas(final PlannedBacklogBySla plannedBacklogBySla,
                                                 final List<DeliveryPromiseProjectionOutput> projections) {

    if (projections.isEmpty()) {
      return plannedBacklogBySla;
    }

    final var deferredSlas = projections.stream()
        .map(DeliveryPromiseProjectionOutput::getDate)
        .map(ZonedDateTime::toInstant)
        .collect(Collectors.toSet());

    final var filteredPlannedUnits = plannedBacklogBySla.getPlannedUnits()
        .entrySet()
        .stream()
        .collect(toMap(
            Map.Entry::getKey,
            entry -> new BacklogBySla(
                entry.getValue()
                    .getDistributions()
                    .stream()
                    .filter(quantity -> !deferredSlas.contains(quantity.getDate()))
                    .collect(toList())
            )
        ));

    return new PlannedBacklogBySla(filteredPlannedUnits);
  }

  private List<DeferralProjectionOutput> getDeferralProjectionsByLoopingOverPossibleBacklogStates(
      final GetDeferralProjectionInput input,
      final List<GetSlaByWarehouseOutput> allCptByWarehouse,
      final Map<ZonedDateTime, Long> cycleTimeByCpt,
      final PlannedBacklogBySla plannedBacklogBySla,
      final ThroughputByHour tph) {

    final Map<ZonedDateTime, DeferralProjectionOutput> results = new HashMap<>();
    final var helper = new BacklogBySlaHelper();

    final var maxCapacity = getMaxCapacity(input);
    final var projectionDates = getInflectionPoints(input);

    List<Backlog> backlogs = input.getBacklog();
    PlannedBacklogBySla plannedBacklog = plannedBacklogBySla;
    for (int sampleIndex = 0; sampleIndex < projectionDates.size() - 1; sampleIndex++) {
      final var projectionDate = projectionDates.get(sampleIndex);

      final List<DeliveryPromiseProjectionOutput> deferredSlas = getDeferredSlas(
          ZonedDateTime.ofInstant(projectionDate, ZoneOffset.UTC),
          backlogs,
          allCptByWarehouse,
          cycleTimeByCpt,
          maxCapacity
      );

      calculateNewlyDeferredSlasProjectionResults(projectionDate, deferredSlas, plannedBacklog, results)
          .forEach(output -> results.put(ZonedDateTime.ofInstant(output.getSla(), ZoneOffset.UTC), output));

      plannedBacklog = removeDeferredSlas(plannedBacklog, deferredSlas);
      backlogs = createNextState(
          backlogs,
          plannedBacklogBySla,
          tph,
          List.of(projectionDate, projectionDates.get(sampleIndex + 1)),
          helper
      );
    }

    return new ArrayList<>(results.values());
  }

  private List<Instant> getInflectionPoints(final GetDeferralProjectionInput input) {
    final var viewDate = input.getViewDate();
    final var numberOfIntervals = (MINUTES.between(input.getDateFrom(), input.getDateTo()) / INTERVAL_WIDTH_MINUTES) + 1;
    final var futureInflectionPoints = LongStream.range(0, numberOfIntervals)
        .mapToObj(i -> input.getDateFrom().plus(i * INTERVAL_WIDTH_MINUTES, MINUTES).toInstant())
        .filter(inflectionPoint -> inflectionPoint.isAfter(viewDate));

    return Stream.concat(Stream.of(viewDate), futureInflectionPoints)
        .collect(toList());
  }

  private Map<ZonedDateTime, Integer> getMaxCapacity(final GetDeferralProjectionInput input) {

    return maxCapacityService.getMaxCapacity(new MaxCapacityInput(
        input.getLogisticCenterId(),
        input.getWorkflow(),
        input.getDateFrom(),
        input.getSlaTo(),
        Collections.emptyList()));
  }

  private PlannedBacklogBySla getIncomingBacklog(final GetDeferralProjectionInput input) {
    final List<PlannedUnits> plannedBacklog = plannedBacklogService.getExpectedBacklog(
        input.getLogisticCenterId(),
        input.getWorkflow(),
        input.getSlaFrom(),
        input.getSlaTo(),
        input.isApplyDeviation()
    );

    return PlannedBacklogBySla.fromPlannedUnits(plannedBacklog);
  }

  private BacklogBySla getCurrentBacklog(final List<Backlog> currentBacklogs) {
    final var quantities = currentBacklogs.stream()
        .map(b -> new QuantityAtDate(b.getDate().toInstant(), b.getQuantity()))
        .collect(toList());

    return new BacklogBySla(quantities);
  }

  private ThroughputByHour getThroughput(final GetDeferralProjectionInput input) {
    final List<EntityOutput> throughput = getThroughputUseCase.execute(GetEntityInput
        .builder()
        .warehouseId(input.getLogisticCenterId())
        .workflow(input.getWorkflow())
        .dateFrom(input.getDateFrom())
        .dateTo(input.getDateTo())
        .processName(List.of(PACKING, PACKING_WALL))
        .source(Source.SIMULATION)
        .build());

    final Map<Instant, Integer> capacity = getCapacityPerHourService.execute(input.getWorkflow(), fromEntityOutputs(throughput))
        .stream()
        .collect(toMap(
            capacityOutput -> capacityOutput.getDate().toInstant(),
            capacityOutput -> (int) capacityOutput.getValue()
        ));

    return new ThroughputByHour(capacity);
  }

  private Stream<DeferralProjectionOutput> calculateNewlyDeferredSlasProjectionResults(
      final Instant projectionDate,
      final List<DeliveryPromiseProjectionOutput> deferredSlas,
      final PlannedBacklogBySla plannedBacklog,
      final Map<ZonedDateTime, DeferralProjectionOutput> results) {

    final var newlyDeferredSlas = deferredSlas.stream()
        .map(DeliveryPromiseProjectionOutput::getDate)
        .filter(sla -> !results.containsKey(sla))
        .map(ZonedDateTime::toInstant)
        .collect(Collectors.toSet());

    if (newlyDeferredSlas.isEmpty()) {
      return Stream.empty();
    } else {
      final var deferredUnitsBySla = getDeferredUnits(plannedBacklog, projectionDate, newlyDeferredSlas);
      return deferredSlas.stream()
          .filter(output -> newlyDeferredSlas.contains(output.getDate().toInstant()))
          .map(output -> {
                final Instant sla = output.getDate().toInstant();
                return new DeferralProjectionOutput(
                    sla,
                    projectionDate,
                    deferredUnitsBySla.get(sla),
                    output.getDeferralStatus()
                );
              }
          );
    }
  }

  // TODO: replace this method with a smarter implementation of PlannedBacklogBySla.get(Instant, Instant) that accepts two dates
  // from different hours.
  private Map<Instant, Integer> getDeferredUnits(final PlannedBacklogBySla plannedBacklog,
                                                 final Instant dateFrom,
                                                 final Set<Instant> deferredSlas) {

    final Map<Instant, Integer> deferredUnitsFromNextHours = plannedBacklog.getPlannedUnits()
        .entrySet()
        .stream()
        .filter(entry -> entry.getKey().isAfter(dateFrom))
        .map(Map.Entry::getValue)
        .map(BacklogBySla::getDistributions)
        .flatMap(List::stream)
        .filter(quantity -> deferredSlas.contains(quantity.getDate()))
        .collect(toMap(QuantityAtDate::getDate, QuantityAtDate::getQuantity, Integer::sum));

    final double minutesFraction = (MINUTES_IN_HOUR - dateFrom.atZone(ZoneOffset.UTC).getMinute()) / MINUTES_IN_HOUR;

    final Map<Instant, Integer> deferredUnitsFromThisHour = Optional.ofNullable(
            plannedBacklog.getPlannedUnits()
                .get(dateFrom.truncatedTo(ChronoUnit.HOURS))
        )
        .map(distributions -> distributions.getDistributions()
            .stream()
            .filter(quantity -> deferredSlas.contains(quantity.getDate()))
            .collect(
                toMap(
                    QuantityAtDate::getDate,
                    quantityAtDate -> (int) (quantityAtDate.getQuantity() * minutesFraction),
                    Integer::sum
                )
            )
        )
        .orElse(Collections.emptyMap());

    return deferredSlas.stream()
        .collect(
            toMap(
                Function.identity(),
                sla -> deferredUnitsFromThisHour.getOrDefault(sla, 0) + deferredUnitsFromNextHours.getOrDefault(sla, 0)
            )
        );
  }
}
