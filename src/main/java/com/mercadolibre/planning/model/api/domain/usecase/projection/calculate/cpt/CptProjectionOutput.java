package com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt;

import com.mercadolibre.planning.model.api.domain.entity.sla.ProcessingTime;
import java.time.ZonedDateTime;
import lombok.Value;

/** The result of a SLA projection. */
@Value
public class CptProjectionOutput {

  private ZonedDateTime date;

  private ZonedDateTime projectedEndDate;

  private int remainingQuantity;

  private ProcessingTime processingTime;
}
