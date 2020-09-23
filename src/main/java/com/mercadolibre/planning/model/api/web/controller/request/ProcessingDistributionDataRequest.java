package com.mercadolibre.planning.model.api.web.controller.request;

import lombok.Value;

import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;

@Value
public class ProcessingDistributionDataRequest {

    @NotNull
    private ZonedDateTime date;

    private long quantity;
}
