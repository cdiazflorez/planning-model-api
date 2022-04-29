package com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt;

import java.util.List;
import lombok.Value;

import java.time.ZonedDateTime;

/**
 * Structure return the result of the projection process.
 */
@Value
public class CptCalculationOutput {

    ZonedDateTime date;

    ZonedDateTime projectedEndDate;

    int remainingQuantity;

    int totalCurrentBacklog;

    int totalPlannedBacklog;

    List<CptCalculationDetailOutput> calculationDetails;
}
