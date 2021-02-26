package com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Builder
@Value
public class SaveForecastDeviationInput {

    private String warehouseId;

    private ZonedDateTime dateFrom;

    private ZonedDateTime dateTo;

    private double value;

    private Long userId;

    private Workflow workflow;

    public CurrentForecastDeviation toCurrentForecastDeviation() {
        return CurrentForecastDeviation
                .builder()
                .logisticCenterId(warehouseId)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .value(value * 0.01)
                .isActive(true)
                .userId(userId)
                .workflow(workflow)
                .build();
    }
}
