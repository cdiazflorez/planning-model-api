package com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.disable;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import lombok.Value;


@Value
public class DisableForecastDeviationInput {

    private String warehouseId;

    private Workflow workflow;
}
