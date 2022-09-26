package com.mercadolibre.planning.model.api.projection.dto;

import java.time.Instant;
import lombok.Value;

@Value
public class ProjectionResult {
  Instant dateOut;

  Long cutOff;

  Instant projectedEndDate;

  int projectedRemainingQuantity;
}
