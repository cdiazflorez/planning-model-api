package com.mercadolibre.planning.model.api.web.controller.projection.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class CptProjectionRequest {

    @NotNull
    private String warehouseId;

    @NotNull
    private ProjectionType type;

    @NotNull
    private List<ProcessName> processName;

    @NotNull
    private ZonedDateTime dateFrom;

    @NotNull
    private ZonedDateTime dateTo;

    private List<QuantityByDate> backlog;
}