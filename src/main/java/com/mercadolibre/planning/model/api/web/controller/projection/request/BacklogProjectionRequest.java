package com.mercadolibre.planning.model.api.web.controller.projection.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import lombok.Value;

import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.List;

@Value
public class BacklogProjectionRequest {

    @NotNull
    private String warehouseId;

    @NotNull
    private List<ProcessName> processName;

    @NotNull
    private ZonedDateTime dateFrom;

    @NotNull
    private ZonedDateTime dateTo;

    private List<CurrentBacklog> currentBacklog;

    private boolean applyDeviation;
}
