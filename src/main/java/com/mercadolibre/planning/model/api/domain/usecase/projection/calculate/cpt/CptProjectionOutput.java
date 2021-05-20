package com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt;

import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class CptProjectionOutput {

    private ZonedDateTime date;

    private ZonedDateTime projectedEndDate;

    private int remainingQuantity;

    private ProcessingTime processingTime;

    private boolean isDeferred;
}
