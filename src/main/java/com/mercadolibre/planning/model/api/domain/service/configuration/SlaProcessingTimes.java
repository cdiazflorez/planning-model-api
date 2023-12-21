package com.mercadolibre.planning.model.api.domain.service.configuration;

import java.time.Instant;
import java.util.List;

public record SlaProcessingTimes(
    int defaultValue,
    List<SlaProperties> properties
) {

  public record SlaProperties(
      Instant sla,
      int processingTime
  ) {
  }
}
