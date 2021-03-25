package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
@Builder
public class GetDeliveryPromiseProjectionInput {

    private String warehouseId;

    private Workflow workflow;

    private ZonedDateTime dateFrom;

    private ZonedDateTime dateTo;

    private List<Backlog> backlog;
}
