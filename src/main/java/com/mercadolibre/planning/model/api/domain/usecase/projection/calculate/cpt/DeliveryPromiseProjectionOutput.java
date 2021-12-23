package com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt;

import com.mercadolibre.planning.model.api.domain.entity.sla.ProcessingTime;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class DeliveryPromiseProjectionOutput {

    private ZonedDateTime date;

    private ZonedDateTime projectedEndDate;

    private int remainingQuantity;

    private ZonedDateTime etdCutoff;

    private ProcessingTime processingTime;

    private ZonedDateTime payBefore;

    private boolean isDeferred;
}
