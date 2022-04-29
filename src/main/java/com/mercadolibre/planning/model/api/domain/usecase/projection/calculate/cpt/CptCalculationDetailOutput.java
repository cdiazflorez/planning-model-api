package com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt;

import java.time.ZonedDateTime;
import lombok.Value;

/**
 * Structure to register the iterations in the projection process.
 */
@Value
public class CptCalculationDetailOutput {
    ZonedDateTime operationHour;

    int unitsBeingProcessed;

    int currentBacklog;
}
