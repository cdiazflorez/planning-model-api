package com.mercadolibre.planning.model.api.web.controller.deviation.request;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save.SaveForecastDeviationInput;
import lombok.Value;

import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;

@Value
public class SaveDeviationRequest {

    @NotNull
    private String warehouseId;

    @NotNull
    private ZonedDateTime dateFrom;

    @NotNull
    private ZonedDateTime dateTo;

    @NotNull
    private double value;

    @NotNull
    private Long userId;

    public SaveForecastDeviationInput toDeviationInput(final Workflow workflow) {
        return  SaveForecastDeviationInput
                    .builder()
                    .warehouseId(warehouseId)
                    .dateFrom(dateFrom)
                    .dateTo(dateTo)
                    .value(value)
                    .userId(userId)
                    .workflow(workflow)
                    .build();
    }
}
