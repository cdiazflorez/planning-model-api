package com.mercadolibre.planning.model.api.web.controller.request;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import lombok.AllArgsConstructor;

import java.time.ZonedDateTime;

@AllArgsConstructor
public class GetEntityRequest {

    private String warehouseId;
    private ZonedDateTime dateFrom;
    private ZonedDateTime dateTo;
    private Source source;

    public GetEntityInput toGetEntityInput(final Workflow workflow, final EntityType entityType) {
        return GetEntityInput.builder()
                .warehouseId(warehouseId)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .source(source)
                .workflow(workflow)
                .entityType(entityType)
                .build();
    }
}
