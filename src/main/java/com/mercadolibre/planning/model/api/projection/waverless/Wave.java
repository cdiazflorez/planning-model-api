package com.mercadolibre.planning.model.api.projection.waverless;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.TriggerName;
import java.time.Instant;
import java.util.Map;
import lombok.Value;

@Value
public class Wave {
  Instant date;

  TriggerName reason;

  Map<ProcessPath, WaveConfiguration> configuration;

  @Value
  public static class WaveConfiguration {
    long lowerBound;

    long upperBound;

    Map<Instant, Long> wavedUnitsByCpt;
  }
}
