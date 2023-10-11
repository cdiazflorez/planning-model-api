package com.mercadolibre.planning.model.api.adapter.staffing;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingMaxDateCreatedByType;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastRepository;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.service.lastupdatedentity.LastEntityModifiedDateService;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class GetLastModifiedDateAdapter implements LastEntityModifiedDateService.GetLastModifiedDateGateway {
  private final GetForecastUseCase getForecastUseCase;
  private final ForecastRepository repository;
  private final CurrentProcessingDistributionRepository currentProcessingDistributionRepository;

  @Override
  public Instant getLastDateStaffingCreated(final String logisticCenterId,
                                            final Workflow workflow,
                                            final Instant viewDate) {
    final Optional<Long> forecastId = getForecastUseCase.execute(
            new GetForecastInput(logisticCenterId, workflow, viewDate, viewDate, viewDate)
        ).stream()
        .max(Comparator.comparing(Long::longValue));

    final Optional<Forecast> forecast = forecastId.flatMap(repository::findById);

    return forecast.stream()
        .map(fc -> fc.getDateCreated().toInstant())
        .findFirst()
        .orElse(null);
  }


  @Override
  public Map<EntityType, Instant> getLastDateEntitiesCreated(final String logisticCenterId,
                                                             final Workflow workflow,
                                                             final Set<EntityType> entityTypes,
                                                             final Instant staffingDateCreated) {

    final Set<ProcessingType> processingTypes = entityTypes.stream()
        .map(EntityType::getProcessingType).collect(Collectors.toSet());

    final List<CurrentProcessingMaxDateCreatedByType> lastDateCreatedEntities = currentProcessingDistributionRepository
        .findDateCreatedByWarehouseIdAndWorkflowAndTypeAndIsActive(
            logisticCenterId,
            workflow,
            processingTypes,
            staffingDateCreated.atZone(ZoneOffset.UTC)
        );

    return entityTypes.stream()
        .filter(entityType -> lastDateCreatedEntities.stream().anyMatch(type -> entityType.getProcessingType() == type.getType()))
        .map(entityType ->
            Map.entry(
                entityType,
                lastDateCreatedEntities.stream()
                    .filter(type -> entityType.getProcessingType() == type.getType())
                    .findFirst()
                    .get().getDateCreated().toInstant()
            ))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

}
