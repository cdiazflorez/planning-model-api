package com.mercadolibre.planning.model.api.web.controller.deviation.request;

import com.mercadolibre.planning.model.api.domain.entity.DeviationType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.disable.DisableForecastDeviationInput;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DisableDeviationRequest {

    @NotNull
    private String warehouseId;


    public DisableForecastDeviationInput toDisableDeviationInput(final Workflow workflow, final DeviationType deviationType) {
        return new DisableForecastDeviationInput(warehouseId, workflow, deviationType, null);
    }
}
