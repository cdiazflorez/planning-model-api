package com.mercadolibre.planning.model.api.web.controller.deviation.request;

import com.mercadolibre.planning.model.api.domain.entity.DeviationType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save.SaveForecastDeviationInput;
import lombok.Value;

import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;

@Value
public class SaveDeviationRequest {

    @NotNull
    String warehouseId;

    @NotNull
    ZonedDateTime dateFrom;

    @NotNull
    ZonedDateTime dateTo;

    @NotNull
    double value;

    @NotNull
    Long userId;

    public SaveForecastDeviationInput toDeviationInput(final Workflow workflow, final DeviationType deviationType) {
        return  SaveForecastDeviationInput
                    .builder()
                    .warehouseId(warehouseId)
                    .dateFrom(dateFrom)
                    .dateTo(dateTo)
                    .value(value)
                    .userId(userId)
                    .workflow(workflow)
                    .deviationType(deviationType)
                    .build();
    }
}
