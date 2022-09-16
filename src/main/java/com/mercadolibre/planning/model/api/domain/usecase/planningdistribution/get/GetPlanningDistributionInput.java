package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.Instant;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GetPlanningDistributionInput {
  String warehouseId;

  Workflow workflow;

  ZonedDateTime dateOutFrom;

  ZonedDateTime dateOutTo;

  ZonedDateTime dateInFrom;

  ZonedDateTime dateInTo;

  boolean applyDeviation;

  Instant viewDate;
}
