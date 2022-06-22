package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity;

import static com.mercadolibre.planning.model.api.util.DateUtils.getCurrentUtcDate;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeService;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.DeliveryPromiseProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeliveryPromiseProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.sla.GetSlaByWarehouseOutboundService;
import com.mercadolibre.planning.model.api.util.TestLogisticCenterMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings({"PMD.ExcessiveImports", "PMD.LongVariable"})
@Slf4j
@AllArgsConstructor
public abstract class GetProjectionUseCase {

  protected final ProcessingDistributionRepository processingDistRepository;

  private final GetForecastUseCase getForecastUseCase;

  private final GetCycleTimeService getCycleTimeService;

  private final GetSlaByWarehouseOutboundService getSlaByWarehouseOutboundService;

  public List<DeliveryPromiseProjectionOutput> execute(final GetDeliveryPromiseProjectionInput input) {

    final List<GetSlaByWarehouseOutput> allCptByWarehouse =
        getSlaByWarehouseOutboundService.execute(new GetSlaByWarehouseInput(TestLogisticCenterMapper
            .toRealLogisticCenter(input.getWarehouseId()),
            input.getDateFrom(),
            input.getDateTo(),
            getCptDefaultFromBacklog(input.getBacklog()),
            input.getTimeZone()));

    final List<ZonedDateTime> slas = Stream.of(
            input.getBacklog()
                .stream()
                .map(Backlog::getDate),
            allCptByWarehouse.stream()
                .map(GetSlaByWarehouseOutput::getDate)
        )
        .flatMap(Function.identity())
        .map(date -> date.withZoneSameInstant(ZoneOffset.UTC))
        .distinct()
        .collect(toList());

    final Map<ZonedDateTime, Long> cycleTimeByCpt = getCycleTimeService.execute(new GetCycleTimeInput(input.getWarehouseId(), slas));

    return DeliveryPromiseCalculator.calculate(
        input.getDateFrom(),
        input.getDateTo(),
        getCurrentUtcDate(),
        input.getBacklog(),
        getMaxCapacity(input),
        allCptByWarehouse,
        cycleTimeByCpt
    );
  }

  protected List<Long> getForecastIds(final GetDeliveryPromiseProjectionInput input) {
    return getForecastUseCase.execute(new GetForecastInput(
        input.getWarehouseId(),
        input.getWorkflow(),
        input.getDateFrom(),
        input.getDateTo()
    ));
  }

  protected Set<Instant> getCapacityHours(final ZonedDateTime dateFrom, final Temporal dateTo) {

    final Duration dur = Duration.between(dateFrom, dateTo);
    return LongStream.range(0, dur.toHours())
        .mapToObj(i -> dateFrom.plusHours(i).truncatedTo(SECONDS).toInstant())
        .collect(toSet());
  }

  private List<ZonedDateTime> getCptDefaultFromBacklog(final List<Backlog> backlogs) {
    return backlogs == null
        ? emptyList()
        : backlogs.stream().map(Backlog::getDate).distinct().collect(toList());
  }

  protected abstract Map<ZonedDateTime, Integer> getMaxCapacity(GetDeliveryPromiseProjectionInput input);

}
