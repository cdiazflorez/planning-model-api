package com.mercadolibre.planning.model.api.web.controller.forecast.request;

import lombok.Value;

import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;

@Value
public class BacklogLimitDataRequest {

    @NotNull
    private ZonedDateTime date;

    @NotNull
    private Integer quantity;

}
