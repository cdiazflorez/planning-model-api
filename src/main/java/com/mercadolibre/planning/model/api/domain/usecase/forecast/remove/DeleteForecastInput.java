package com.mercadolibre.planning.model.api.domain.usecase.forecast.remove;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import lombok.Value;

@Value
public class DeleteForecastInput {
    private Workflow workflow;
    private Integer weeks;
}
