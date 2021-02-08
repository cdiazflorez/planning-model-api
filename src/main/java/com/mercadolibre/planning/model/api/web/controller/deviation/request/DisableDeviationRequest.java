package com.mercadolibre.planning.model.api.web.controller.deviation.request;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.disable.DisableForecastDeviationInput;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class DisableDeviationRequest {

    @NotNull
    private String warehouseId;

    public DisableForecastDeviationInput toDisableDeviationInput(final Workflow workflow) {
        return new DisableForecastDeviationInput(warehouseId, workflow);
    }
}
