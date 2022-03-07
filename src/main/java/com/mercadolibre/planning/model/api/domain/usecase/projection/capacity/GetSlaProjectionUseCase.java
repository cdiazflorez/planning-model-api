package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity;

import static com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityInput.fromEntityOutputs;
import static com.mercadolibre.planning.model.api.util.DateUtils.getCurrentUtcDate;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.entity.sla.ProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedBacklogService;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedUnits;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.GetCapacityPerHourService;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeService;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptCalculationOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.SlaProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetSlaProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.sla.GetSlaByWarehouseInboundService;
import com.mercadolibre.planning.model.api.domain.usecase.sla.GetSlaByWarehouseOutboundService;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/** SLA projection by Workflow. */
@Component
@AllArgsConstructor
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.LongVariable"})
public class GetSlaProjectionUseCase {

  private final CalculateCptProjectionUseCase calculateCptProjection;

  private final GetThroughputUseCase getThroughputUseCase;

  private final GetCapacityPerHourService getCapacityPerHourService;

  private final GetSlaByWarehouseOutboundService getSlaByWarehouseOutboundService;

  private final GetSlaByWarehouseInboundService getSlaByWarehouseInboundService;

  private final PlannedBacklogService plannedBacklogService;

  private final GetCycleTimeService getCycleTimeService;

  /**
   * Calculates the SLAs projections for a Logistic Center and Workflow.
   *
   * <p>
   * This method does not calculate the projections but
   * retrieves and maps the necessary inputs to invoke the projection calculation service.
   * Also, enriches the response with SLAs' processing times.
   * </p>
   *
   * <p>
   * SLA projections' are concerned with projecting the end date and remaining quantities - if any - for each SLA.
   * </p>
   *
   * <p>
   * The SLAs to be projected are determined based on the backlog and from the configuration retrieved from logistics centers.
   * </p>
   *
   * @param  request encapsulates the parameters for this use case.
   * @return         projections for each SLA.
   */
  public List<CptProjectionOutput> execute(final GetSlaProjectionInput request) {
    final Workflow workflow = request.getWorkflow();
    final String warehouseId = request.getWarehouseId();
    final ZonedDateTime dateFrom = request.getDateFrom();
    final ZonedDateTime dateTo = request.getDateTo();
    final String timeZone = request.getTimeZone();

    final List<EntityOutput> throughput = getThroughputUseCase.execute(GetEntityInput
        .builder()
        .warehouseId(warehouseId)
        .workflow(workflow)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .processName(request.getProcessName())
        .simulations(request.getSimulations())
        .source(request.getSource())
        .build());

    final Map<ZonedDateTime, Integer> capacity = getCapacityPerHourService
        .execute(workflow, fromEntityOutputs(throughput))
        .stream()
        .collect(toMap(
            CapacityOutput::getDate,
            capacityOutput -> (int) capacityOutput.getValue()
        ));

    final List<PlannedUnits> expectedBacklog = plannedBacklogService.getExpectedBacklog(
        request.getWarehouseId(),
        workflow,
        request.getDateFrom(),
        request.getDateTo(),
        request.isApplyDeviation()
    );

    final List<GetSlaByWarehouseOutput> slaByWarehouse = slaByWarehouseAndWorkflow(
        workflow,
        warehouseId,
        dateFrom,
        dateTo,
        request.getBacklog(),
        expectedBacklog,
        timeZone);

    final List<CptCalculationOutput> cptProjectionOutputs =
        calculateCptProjection.execute(SlaProjectionInput.builder()
            .workflow(workflow)
            .logisticCenterId(warehouseId)
            .dateFrom(dateFrom)
            .dateTo(dateTo)
            .capacity(capacity)
            .backlog(getBacklog(request.getBacklog()))
            .plannedUnits(expectedBacklog)
            .slaByWarehouse(slaByWarehouse)
            .currentDate(getCurrentUtcDate())
            .build());

    final Map<ZonedDateTime, Long> cycleTimeByCpt = getCycleTimes(
        request.getWarehouseId(),
        request.getWorkflow(),
        cptProjectionOutputs.stream()
            .map(CptCalculationOutput::getDate)
            .collect(toList())
    );

    return cptProjectionOutputs.stream()
        .map(item -> new CptProjectionOutput(
                item.getDate(),
                item.getProjectedEndDate(),
                item.getRemainingQuantity(),
                buildProcessingTime(cycleTimeByCpt, item.getDate())
            )
        ).collect(toList());
  }

  private List<Backlog> getBacklog(final List<QuantityByDate> backlogs) {
    return backlogs == null
        ? emptyList()
        : backlogs.stream().map(QuantityByDate::toBacklog).collect(toList());
  }

  private List<ZonedDateTime> getCptDefaultFromBacklog(final List<QuantityByDate> backlogs) {
    return backlogs == null
        ? emptyList()
        : backlogs.stream().map(QuantityByDate::getDate).distinct().collect(toList());
  }

  private List<ZonedDateTime> getDefaultSlasFromExpectedUnits(final List<PlannedUnits> plannedUnits) {
    return plannedUnits == null
        ? emptyList()
        : plannedUnits.stream()
        .map(PlannedUnits::getDateOut)
        .map(date -> date.withZoneSameInstant(ZoneId.of("UTC")))
        .distinct()
        .collect(toList());
  }

  private List<GetSlaByWarehouseOutput> slaByWarehouseAndWorkflow(
      final Workflow workflow,
      final String warehouseId,
      final ZonedDateTime dateFrom,
      final ZonedDateTime dateTo,
      final List<QuantityByDate> backlog,
      final List<PlannedUnits> plannedUnits,
      final String timeZone) {

    if (workflow == Workflow.FBM_WMS_OUTBOUND) {
      final GetSlaByWarehouseInput getSlaByWarehouseInput = new GetSlaByWarehouseInput(
          warehouseId, dateFrom, dateTo, getCptDefaultFromBacklog(backlog), timeZone
      );

      return getSlaByWarehouseOutboundService.execute(getSlaByWarehouseInput);
    } else if (workflow == Workflow.FBM_WMS_INBOUND) {
      final List<ZonedDateTime> defaults = Stream.concat(
              getCptDefaultFromBacklog(backlog).stream(),
              getDefaultSlasFromExpectedUnits(plannedUnits).stream()
          )
          .distinct()
          .collect(toList());

      final GetSlaByWarehouseInput getSlaByWarehouseInput = new GetSlaByWarehouseInput(
          warehouseId, dateFrom, dateTo, defaults, timeZone
      );

      return getSlaByWarehouseInboundService.execute(getSlaByWarehouseInput);
    }
    return emptyList();
  }

  private Map<ZonedDateTime, Long> getCycleTimes(final String warehouseId,
                                                          final Workflow workflow,
                                                          final List<ZonedDateTime> dates) {
    if (workflow == Workflow.FBM_WMS_OUTBOUND) {
      return getCycleTimeService.execute(new GetCycleTimeInput(warehouseId, dates));
    }

    return emptyMap();
  }

  private ProcessingTime buildProcessingTime(final Map<ZonedDateTime, Long> cycleTimeByCpt,
                                             final ZonedDateTime date) {

    return Optional.ofNullable(cycleTimeByCpt.get(date))
        .map(ct -> new ProcessingTime(ct, MetricUnit.MINUTES))
        .orElse(null);
  }
}
