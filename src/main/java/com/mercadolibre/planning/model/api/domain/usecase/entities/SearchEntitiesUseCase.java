package com.mercadolibre.planning.model.api.domain.usecase.entities;

import static com.mercadolibre.planning.model.api.domain.usecase.entities.input.EntitySearchFilters.ABILITY_LEVEL;
import static com.mercadolibre.planning.model.api.domain.usecase.entities.input.EntitySearchFilters.PROCESSING_TYPE;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.input.SearchEntitiesInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityInput;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@AllArgsConstructor
public class SearchEntitiesUseCase implements UseCase<SearchEntitiesInput, Map<EntityType, Object>> {

  private final GetEntitiesStrategy getEntitiesStrategy;

  @Override
  public Map<EntityType, Object> execute(final SearchEntitiesInput input) {
    return input.getEntityTypes().stream()
        .collect(
            toMap(
                Function.identity(),
                entityType -> getEntitiesStrategy
                    .getBy(entityType)
                    .map(service -> service.execute(createEntityInputFrom(input, entityType)))
                    .orElseThrow()
            )
        );
  }

  private GetEntityInput createEntityInputFrom(final SearchEntitiesInput searchEntitiesInput,
                                               final EntityType entityType) {
    final var processPaths = CollectionUtils.isEmpty(searchEntitiesInput.getProcessPaths())
        ? List.of(ProcessPath.GLOBAL)
        : searchEntitiesInput.getProcessPaths();
    return switch (entityType) {
      case HEADCOUNT -> GetHeadcountInput.builder()
          .warehouseId(searchEntitiesInput.getWarehouseId())
          .workflow(searchEntitiesInput.getWorkflow())
          .dateFrom(searchEntitiesInput.getDateFrom())
          .dateTo(searchEntitiesInput.getDateTo())
          .entityType(entityType)
          .processName(searchEntitiesInput.getProcessName())
          .source(searchEntitiesInput.getSource())
          .simulations(searchEntitiesInput.getSimulations())
          .processingType(searchEntitiesInput.getEntityFilters()
              .getOrDefault(HEADCOUNT, null)
              .getOrDefault(PROCESSING_TYPE.toJson(), null).stream()
              .map(ProcessingType::of)
              .map(Optional::get)
              .collect(toSet()))
          .viewDate(searchEntitiesInput.getViewDate())
          .processPaths(processPaths)
          .build();
      case PRODUCTIVITY -> GetProductivityInput.builder()
          .warehouseId(searchEntitiesInput.getWarehouseId())
          .workflow(searchEntitiesInput.getWorkflow())
          .dateFrom(searchEntitiesInput.getDateFrom())
          .dateTo(searchEntitiesInput.getDateTo())
          .entityType(entityType)
          .processName(searchEntitiesInput.getProcessName())
          .source(searchEntitiesInput.getSource())
          .simulations(searchEntitiesInput.getSimulations())
          .abilityLevel(searchEntitiesInput.getEntityFilters()
              .getOrDefault(PRODUCTIVITY, null)
              .getOrDefault(ABILITY_LEVEL.toJson(), null).stream()
              .map(Integer::valueOf)
              .collect(toSet()))
          .viewDate(searchEntitiesInput.getViewDate())
          .processPaths(processPaths)
          .build();
      default -> GetEntityInput.builder()
          .warehouseId(searchEntitiesInput.getWarehouseId())
          .workflow(searchEntitiesInput.getWorkflow())
          .dateFrom(searchEntitiesInput.getDateFrom())
          .dateTo(searchEntitiesInput.getDateTo())
          .entityType(entityType)
          .processName(searchEntitiesInput.getProcessName())
          .source(searchEntitiesInput.getSource())
          .simulations(searchEntitiesInput.getSimulations())
          .viewDate(searchEntitiesInput.getViewDate())
          .processPaths(processPaths)
          .build();
    };
  }
}
