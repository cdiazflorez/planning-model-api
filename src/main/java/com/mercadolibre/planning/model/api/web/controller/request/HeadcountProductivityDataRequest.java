package com.mercadolibre.planning.model.api.web.controller.request;

import lombok.Value;

import javax.validation.constraints.NotNull;

import java.time.OffsetTime;

@Value
public class HeadcountProductivityDataRequest {

    @NotNull
    private OffsetTime dayTime;

    private long productivity;
}
