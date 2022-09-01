package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetPolyvalenceForecastMetadata;
import com.mercadolibre.planning.model.api.exception.ForecastNotFoundException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LastForecastRepositoryImp implements GetPolyvalenceForecastMetadata.LastForecastRepository {

  private final ForecastRepository forecastRepository;

  @Override
  public Long getForecastByWorkflow(final String logisticCenterId, final Workflow workflow, final String week) {

    final ForecastIdView forecastIdView = forecastRepository
        .findLastForecastIdByWarehouseIdAAndWorkflowAndWeeks(logisticCenterId, workflow.name(), Set.of(week))
        .stream()
        .findAny()
        .orElse(null);

    if (forecastIdView == null) {
      throw new ForecastNotFoundException(workflow.name(), logisticCenterId, Set.of(week));
    }

    return forecastIdView.getId();
  }
}
