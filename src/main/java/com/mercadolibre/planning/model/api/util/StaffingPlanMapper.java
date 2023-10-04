package com.mercadolibre.planning.model.api.util;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS_NS;
import static com.mercadolibre.planning.model.api.util.EntitiesUtil.toMapByProcessPathProcessNameDateAndSource;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityInput;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class StaffingPlanMapper {

  private static final int MAIN_ABILITY = 1;

  private static final int POLYVALENT_ABILITY = 2;

  private StaffingPlanMapper() {
    throw new IllegalStateException("Utility class");
  }

  public static GetProductivityInput createProductivityInput(final GetEntityInput input) {
    return GetProductivityInput.builder()
        .warehouseId(input.getWarehouseId())
        .workflow(input.getWorkflow())
        .entityType(PRODUCTIVITY)
        .dateFrom(input.getDateFrom())
        .dateTo(input.getDateTo())
        .source(input.getSource())
        .processPaths(input.getProcessPaths())
        .processName(input.getProcessName())
        .simulations(input.getSimulations())
        .abilityLevel(Set.of(MAIN_ABILITY, POLYVALENT_ABILITY))
        .viewDate(input.getViewDate())
        .build();
  }

  public static GetHeadcountInput createSystemicHeadcountInput(final GetEntityInput input) {
    return createHeadcountInput(input, EFFECTIVE_WORKERS);
  }

  public static GetHeadcountInput createNonSystemicHeadcountInput(final GetEntityInput input) {
    return createHeadcountInput(input, EFFECTIVE_WORKERS_NS);
  }

  private static GetHeadcountInput createHeadcountInput(final GetEntityInput input, final ProcessingType processingType) {
    return GetHeadcountInput.builder()
        .warehouseId(input.getWarehouseId())
        .workflow(input.getWorkflow())
        .entityType(HEADCOUNT)
        .dateFrom(input.getDateFrom())
        .dateTo(input.getDateTo())
        .source(input.getSource())
        .processPaths(input.getProcessPaths())
        .processName(input.getProcessName())
        .simulations(input.getSimulations())
        .processingType(Set.of(processingType)
        )
        .viewDate(input.getViewDate())
        .build();
  }

  public static Map<ProcessPath, Map<ProcessName, Map<Instant, StaffingPlanMetrics>>> adaptThroughputResponse(
      final List<EntityOutput> throughputList
  ) {
    return throughputList.stream().collect(
        Collectors.groupingBy(EntityOutput::getProcessPath,
            Collectors.groupingBy(EntityOutput::getProcessName,
                Collectors.toMap(entity -> entity.getDate().toInstant(),
                    entity -> new StaffingPlanMetrics(entity.getRoundedValue(), entity.getRoundedOriginalValue())
                )
            )
        )
    );
  }

  public static <T extends EntityOutput> Map<ProcessPath, Map<ProcessName, Map<Instant, StaffingPlanMetrics>>> adaptEntityOutputResponse(
      final List<T> entityOutputList
  ) {
    final var entityOutputBySource = toMapByProcessPathProcessNameDateAndSource(entityOutputList);

    return entityOutputList.stream()
        .filter(entityOutput -> entityOutput.getSource().equals(Source.FORECAST))
        .collect(
            Collectors.groupingBy(EntityOutput::getProcessPath,
                Collectors.groupingBy(EntityOutput::getProcessName,
                    Collectors.toMap(
                        entity -> entity.getDate().toInstant(),
                        entity -> createStaffingPlan(entityOutputBySource, entity),
                        (staffingPlanMetrics, staffingPlanMetrics2) -> staffingPlanMetrics
                    )
                )
            )
        );
  }

  private static <T extends EntityOutput> StaffingPlanMetrics createStaffingPlan(
      final Map<ProcessPath, Map<ProcessName, Map<ZonedDateTime, Map<Source, T>>>> entityOutputBySource,
      final T entity
  ) {
    final var eo = entityOutputBySource
        .get(entity.getProcessPath())
        .get(entity.getProcessName())
        .get(entity.getDate().withFixedOffsetZone());
    final double originalValue = eo.get(Source.FORECAST).getValue();
    final double value = eo.get(Source.SIMULATION) != null
        ? eo.get(Source.SIMULATION).getRoundedValue()
        : originalValue;
    return new StaffingPlanMetrics(value, originalValue);
  }

  public record StaffingPlan(
      Map<ProcessPath, Map<ProcessName, Map<Instant, StaffingPlanMapper.StaffingPlanMetrics>>> systemicHeadcount,
      Map<ProcessPath, Map<ProcessName, Map<Instant, StaffingPlanMapper.StaffingPlanMetrics>>> nonSystemicHeadcount,
      Map<ProcessPath, Map<ProcessName, Map<Instant, StaffingPlanMapper.StaffingPlanMetrics>>> productivity,
      Map<ProcessPath, Map<ProcessName, Map<Instant, StaffingPlanMapper.StaffingPlanMetrics>>> throughput
  ) {
  }

  public record StaffingPlanMetrics(double quantity, double originalQuantity) {
  }
}
