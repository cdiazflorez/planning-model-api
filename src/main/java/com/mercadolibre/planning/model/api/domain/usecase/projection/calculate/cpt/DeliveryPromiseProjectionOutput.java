package com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt;

import com.mercadolibre.planning.model.api.domain.entity.sla.ProcessingTime;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class DeliveryPromiseProjectionOutput {

    ZonedDateTime date;

    ZonedDateTime projectedEndDate;

    int remainingQuantity;

    ZonedDateTime etdCutoff;

    ProcessingTime processingTime;

    ZonedDateTime payBefore;

    boolean isDeferred;
}
