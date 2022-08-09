package com.mercadolibre.planning.model.api.web.controller.projection.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class BacklogProjectionRequest {

    @NotNull
    String warehouseId;

    @NotNull
    List<ProcessName> processName;

    @NotNull
    ZonedDateTime dateFrom;

    @NotNull
    ZonedDateTime dateTo;

    List<CurrentBacklog> currentBacklog;

    boolean applyDeviation;

    Map<ZonedDateTime, Double> packingWallRatios;
}
