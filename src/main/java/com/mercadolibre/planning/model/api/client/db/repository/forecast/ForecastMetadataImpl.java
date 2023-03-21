package com.mercadolibre.planning.model.api.client.db.repository.forecast;


import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadata.LastForecastRepository;
import com.mercadolibre.planning.model.api.exception.ForecastNotFoundException;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ForecastMetadataImpl implements LastForecastRepository {

  private final ForecastRepository forecastRepository;

  @Override
  public Long getForecastByWorkflow(String logisticCenterId, Workflow workflow, String week) {
    final ForecastIdView forecastIdView = forecastRepository
        .findForecastIdsByWarehouseIdAAndWorkflowAndWeeksAtViewDate(logisticCenterId, workflow.name(), Set.of(week), Instant.now())
        .stream()
        .findAny()
        .orElseThrow(() -> new ForecastNotFoundException(workflow.name(), logisticCenterId, Set.of(week)));

    return forecastIdView.getId();
  }
}
