package com.mercadolibre.planning.model.api.web.controller.request;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class GetEntityRequest {

    @NotBlank
    private String warehouseId;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime dateFrom;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime dateTo;

    @NotNull
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
