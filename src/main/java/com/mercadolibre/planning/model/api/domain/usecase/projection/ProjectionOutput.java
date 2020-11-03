package com.mercadolibre.planning.model.api.domain.usecase.projection;

import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class ProjectionOutput {

    private ZonedDateTime date;

    private ZonedDateTime projectedEndDate;

    private int remainingQuantity;
}
