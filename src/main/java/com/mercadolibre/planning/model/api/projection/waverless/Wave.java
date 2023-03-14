package com.mercadolibre.planning.model.api.projection.waverless;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.util.Map;
import lombok.Value;

@Value
public class Wave {
  Instant date;

  Map<ProcessPath, WaveConfiguration> configuration;

  @Value
  public static class WaveConfiguration {
    long lowerBound;

    long upperBound;

    Map<Instant, Long> wavedUnitsByCpt;
  }
}
