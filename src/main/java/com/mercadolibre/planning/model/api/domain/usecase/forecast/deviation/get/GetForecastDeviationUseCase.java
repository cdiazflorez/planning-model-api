package com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.get;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.exception.EntityNotFoundException;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.GetForecastDeviationResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.PERCENTAGE;
import static java.lang.Math.round;

@Service
@AllArgsConstructor
public class GetForecastDeviationUseCase implements UseCase<GetForecastDeviationInput,
        GetForecastDeviationResponse> {

    private final CurrentForecastDeviationRepository deviationRepository;

    @Override
    public GetForecastDeviationResponse execute(final GetForecastDeviationInput input) {

        final CurrentForecastDeviation deviation = deviationRepository
                    .findByLogisticCenterIdAndWorkflowAndIsActiveTrueAndDateToIsGreaterThanEqual(
                            input.getWarehouseId(),
                            input.getWorkflow(),
                            input.getDate().withFixedOffsetZone())
                    .orElseThrow(notFoundException(input.getWarehouseId()));

        return deviation == null
                ? null
                : GetForecastDeviationResponse.builder()
                .dateFrom(deviation.getDateFrom())
                .dateTo(deviation.getDateTo())
                .value(round((deviation.getValue() * 100) * 10) / 10.0)
                .metricUnit(PERCENTAGE)
                .build();
    }

    private Supplier<EntityNotFoundException> notFoundException(final String warehouseId) {
        return () -> new EntityNotFoundException("CurrentForecastDeviation", warehouseId);
    }
}
