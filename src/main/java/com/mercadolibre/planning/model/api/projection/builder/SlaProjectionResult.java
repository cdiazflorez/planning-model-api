package com.mercadolibre.planning.model.api.projection.builder;

import java.time.Instant;
import java.util.List;

public record SlaProjectionResult(
    List<Sla> slas
) {
  public record Sla(
      Instant date,
      Instant projectedEndDate,
      Double remainingQuantity) {
  }
}
