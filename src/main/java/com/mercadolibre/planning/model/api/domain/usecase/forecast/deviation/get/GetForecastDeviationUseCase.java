package com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.get;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.PERCENTAGE;
import static java.lang.Math.round;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.GetForecastDeviationResponse;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GetForecastDeviationUseCase {

  private static final int ROUNDING_FACTOR = 100;

  private final CurrentForecastDeviationRepository deviationRepository;

  private static GetForecastDeviationResponse toResponse(final CurrentForecastDeviation deviation) {
    return GetForecastDeviationResponse.builder()
        .workflow(deviation.getWorkflow())
        .dateFrom(deviation.getDateFrom())
        .dateTo(deviation.getDateTo())
        .value(round((deviation.getValue() * ROUNDING_FACTOR) * 10.0) / 10.0)
        .metricUnit(PERCENTAGE)
        .path(deviation.getPath())
        .type(deviation.getType())
        .build();
  }

  public Optional<GetForecastDeviationResponse> execute(final GetForecastDeviationInput input) {
    return deviationRepository
        .findByLogisticCenterIdAndWorkflowAndIsActiveTrueAndDateToIsGreaterThanEqual(
            input.getWarehouseId(),
            input.getWorkflow(),
            input.getDate().withFixedOffsetZone()
        )
        .map(GetForecastDeviationUseCase::toResponse);
  }

}
