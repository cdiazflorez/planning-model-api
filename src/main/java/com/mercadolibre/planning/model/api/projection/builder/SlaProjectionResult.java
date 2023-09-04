package com.mercadolibre.planning.model.api.projection.builder;

import java.time.Instant;
import java.util.List;
import lombok.Value;

@Value
public class SlaProjectionResult {
  List<Sla> slas;

  @Value
  public static class Sla {
    Instant date;

    Instant projectedEndDate;

    Double remainingQuantity;
  }
}
