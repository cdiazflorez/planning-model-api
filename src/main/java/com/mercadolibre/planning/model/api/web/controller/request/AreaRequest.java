package com.mercadolibre.planning.model.api.web.controller.request;

import lombok.Value;

import javax.validation.constraints.NotBlank;

@Value
public class AreaRequest {

    @NotBlank
    private String areaId;

    private double quantity;
}
