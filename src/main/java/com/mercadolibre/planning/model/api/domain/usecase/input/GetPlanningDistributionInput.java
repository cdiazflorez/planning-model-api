package com.mercadolibre.planning.model.api.domain.usecase.input;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Builder
@Value
public class GetPlanningDistributionInput {

    private String warehouseId;
    private Workflow workflow;
    private ZonedDateTime dateFrom;
    private ZonedDateTime dateTo;
}
