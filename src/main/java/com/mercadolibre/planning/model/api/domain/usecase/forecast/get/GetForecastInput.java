package com.mercadolibre.planning.model.api.domain.usecase.forecast.get;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
@AllArgsConstructor
public class GetForecastInput {
    private String warehouseId;
    private Workflow workflow;
    private ZonedDateTime dateFrom;
    private ZonedDateTime dateTo;
}
