package com.mercadolibre.planning.model.api.web.controller.plan.staffing;

import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest.AbilityLevel.MAIN;
import static com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest.AbilityLevel.POLYVALENT;
import static com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest.Groupers.ABILITY_LEVEL;
import static com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest.Groupers.HEADCOUNT_TYPE;
import static com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest.HeadcountType.NON_SYSTEMIC;
import static com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest.HeadcountType.SYSTEMIC;
import static java.util.stream.Collectors.groupingBy;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.ProductivityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.exception.EntityTypeNotSupportedException;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adapter for Staffing Plan.
 *
 * <p>brings together the different use cases to obtain the necessary information for the staffing plan.</p>
 */
@AllArgsConstructor
@Component
public class StaffingPlanAdapter {

  private static final List<EntityType> ALLOWED_ENTITIES = List.of(HEADCOUNT, PRODUCTIVITY, THROUGHPUT, MAX_CAPACITY);

  private final GetHeadcountEntityUseCase headcountUseCase;

  private final GetProductivityEntityUseCase productivityUseCase;

  private final GetThroughputUseCase throughputUseCase;

  public StaffingPlanResponse getStaffingPlan(final StaffingPlanRequest request) {
    checkAllowedEntities(request);

    final List<Resource> resources = request.resources().stream()
        .map(entityType -> getResource(request, entityType))
        .toList();

    return new StaffingPlanResponse(resources);
  }

  private void checkAllowedEntities(final StaffingPlanRequest request) {
    request.resources().forEach(
        entityType -> {
          if (!ALLOWED_ENTITIES.contains(entityType)) {
            throw new EntityTypeNotSupportedException(entityType);
          }
        }
    );
  }

  private Resource getResource(final StaffingPlanRequest request, final EntityType entityType) {
    switch (entityType) {
      case HEADCOUNT -> {
        return getHeadcountResource(request);
      }
      case PRODUCTIVITY -> {
        return getProductivityResource(request);
      }
      case THROUGHPUT -> {
        return getThroughputResource(request);
      }
      case MAX_CAPACITY -> {
        return getMaxCapacityResource(request);
      }
      default -> throw new EntityTypeNotSupportedException(entityType);
    }
  }

  private Resource getThroughputResource(StaffingPlanRequest request) {

    final var response = throughputUseCase.execute(
        GetEntityInput.builder()
            .warehouseId(request.warehouseId())
            .workflow(request.workflow())
            .entityType(THROUGHPUT)
            .dateFrom(request.dateFrom())
            .dateTo(request.dateTo())
            .processPaths(request.getProcessPathAsEnum())
            .processName(request.processes())
            .viewDate(request.viewDate())
            .build()
    );
    return createResourceFromEntityOutput(THROUGHPUT, request.groupers(), response);
  }

  private Resource getMaxCapacityResource(StaffingPlanRequest request) {

    final var response = headcountUseCase.execute(createHeadcountInput(request, Set.of(ProcessingType.MAX_CAPACITY), null));
    return createResourceFromEntityOutput(MAX_CAPACITY, request.groupers(), response);

  }

  private Resource getHeadcountResource(final StaffingPlanRequest request) {

    final List<StaffingPlanRequest.HeadcountType> processingTypesFilter = request.headcountTypes().isEmpty()
        ? List.of(SYSTEMIC)
        : request.headcountTypes();

    final List<StaffingPlanRequest.HeadcountType> processingTypes = request.groupers().contains(HEADCOUNT_TYPE)
        ? List.of(SYSTEMIC, NON_SYSTEMIC)
        : processingTypesFilter;

    final List<EntityOutput> result = headcountUseCase.execute(
        createHeadcountInput(
            request,
            processingTypes.stream().map(StaffingPlanRequest.HeadcountType::getValue).collect(Collectors.toSet()),
            request.getProcessPathAsEnum()
        )
    );

    return createResourceFromEntityOutput(HEADCOUNT, request.groupers(), result);
  }

  private Resource getProductivityResource(final StaffingPlanRequest request) {

    final List<StaffingPlanRequest.AbilityLevel> abilityLevelFilter = request.abilityLevels().isEmpty()
        ? List.of(MAIN)
        : request.abilityLevels();

    final List<StaffingPlanRequest.AbilityLevel> abilityLevels = request.groupers().contains(ABILITY_LEVEL)
        ? List.of(MAIN, POLYVALENT)
        : abilityLevelFilter;

    final List<ProductivityOutput> result = productivityUseCase.execute(
        GetProductivityInput.builder()
            .warehouseId(request.warehouseId())
            .workflow(request.workflow())
            .entityType(PRODUCTIVITY)
            .dateFrom(request.dateFrom())
            .dateTo(request.dateTo())
            .processPaths(request.getProcessPathAsEnum())
            .processName(request.processes())
            .source(Source.SIMULATION)
            .abilityLevel(abilityLevels.stream().map(StaffingPlanRequest.AbilityLevel::getValue).collect(Collectors.toSet()))
            .viewDate(request.viewDate())
            .build()
    );
    return createResourceFromEntityOutput(PRODUCTIVITY, request.groupers(), result);
  }

  private <T extends EntityOutput> Resource createResourceFromEntityOutput(
      final EntityType entityType,
      final List<StaffingPlanRequest.Groupers> groupers,
      final List<T> entitiesResult
  ) {
    final var valuesGroupByGroupers = entitiesResult.stream()
        .collect(
            groupingBy(entityOutput -> groupers.stream()
                .collect(
                    Collectors.toMap(
                        StaffingPlanRequest.Groupers::toJson,
                        grouper -> grouper.getValueGetter().apply(entityOutput)
                    )
                )
            )
        );
    final var values = valuesGroupByGroupers.entrySet().stream()
        .map(this::createResourceValues).toList();
    return new Resource(entityType, values);
  }

  private <T extends EntityOutput> ResourceValues createResourceValues(final Map.Entry<Map<String, String>, List<T>> entry) {
    final var originalValueList = entry.getValue().stream()
        .filter(entityOutput -> entityOutput.getSource() == Source.FORECAST)
        .toList();

    final var valuesList = entry.getValue().stream()
        .filter(entityOutput -> entityOutput.getSource() == Source.SIMULATION)
        .toList();

    final var originalValue = originalValueList.isEmpty()
        ? valuesList.stream().mapToDouble(EntityOutput::getOriginalValue).sum()
        : originalValueList.stream().mapToDouble(EntityOutput::getValue).sum();

    return new ResourceValues(
        valuesList.isEmpty() ? originalValue : valuesList.stream().mapToDouble(EntityOutput::getValue).sum(),
        originalValue,
        entry.getKey()
    );
  }

  private GetHeadcountInput createHeadcountInput(
      final StaffingPlanRequest request,
      final Set<ProcessingType> processingTypes,
      final List<ProcessPath> processPaths
  ) {

    return GetHeadcountInput.builder()
        .warehouseId(request.warehouseId())
        .workflow(request.workflow())
        .entityType(HEADCOUNT)
        .dateFrom(request.dateFrom())
        .dateTo(request.dateTo())
        .processPaths(processPaths)
        .processName(request.processes())
        .processingType(processingTypes)
        .source(Source.SIMULATION)
        .viewDate(request.viewDate())
        .build();
  }

  public record StaffingPlanResponse(List<Resource> resources) {
  }

  public record Resource(EntityType name, List<ResourceValues> values) {
  }

  public record ResourceValues(double value, double originalValue, Map<String, String> groupers) {
  }
}
