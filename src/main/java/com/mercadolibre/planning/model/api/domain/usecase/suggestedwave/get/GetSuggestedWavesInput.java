package com.mercadolibre.planning.model.api.domain.usecase.suggestedwave.get;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class GetSuggestedWavesInput {

    private String warehouseId;
    private Workflow workflow;
    private ZonedDateTime dateFrom;
    private ZonedDateTime dateTo;
    private long backlog;

}
