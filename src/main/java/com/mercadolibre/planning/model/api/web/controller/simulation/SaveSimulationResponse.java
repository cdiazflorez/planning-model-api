package com.mercadolibre.planning.model.api.web.controller.simulation;

import com.mercadolibre.planning.model.api.domain.entity.sla.ProcessingTime;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class SaveSimulationResponse {

    private ZonedDateTime date;

    private ZonedDateTime projectedEndDate;

    private int remainingQuantity;

    private ProcessingTime processingTime;

    private boolean isDeferred;

}
