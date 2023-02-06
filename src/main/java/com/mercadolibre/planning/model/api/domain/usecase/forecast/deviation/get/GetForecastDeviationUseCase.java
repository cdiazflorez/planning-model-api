package com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.get;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.PERCENTAGE;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.GetForecastDeviationResponse;
import java.util.List;
import java.util.stream.Collectors;
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
        .value(deviation.getValue())
        .metricUnit(PERCENTAGE)
        .path(deviation.getPath())
        .type(deviation.getType())
        .build();
  }

  public List<GetForecastDeviationResponse> execute(final GetForecastDeviationInput input) {
    return deviationRepository
        .findByLogisticCenterIdAndWorkflowAndIsActiveTrueAndDateToIsGreaterThanEqual(
            input.getWarehouseId(),
            input.getWorkflow(),
            input.getDate().withFixedOffsetZone()
        ).stream()
        .map(GetForecastDeviationUseCase::toResponse)
        .collect(Collectors.toList());
  }

}
