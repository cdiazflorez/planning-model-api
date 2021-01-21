package com.mercadolibre.planning.model.api.web.controller.forecast.request;

import lombok.Value;

import javax.validation.constraints.NotBlank;

@Value
public class AreaRequest {

    @NotBlank
    private String areaId;

    private long quantity;
}
