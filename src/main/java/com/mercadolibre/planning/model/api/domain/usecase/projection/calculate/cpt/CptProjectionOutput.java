package com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class CptProjectionOutput {

    private ZonedDateTime date;

    private ZonedDateTime projectedEndDate;

    private int remainingQuantity;

    private ProcessingTime processingTime;

    private boolean isDeferred;
}
