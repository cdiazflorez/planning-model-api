package com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.get;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class GetForecastDeviationInput {

    private String warehouseId;
    private Workflow workflow;
    private ZonedDateTime date;

}
