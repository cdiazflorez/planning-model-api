package com.mercadolibre.planning.model.api.projection.waverless;

import java.time.Instant;
import java.util.Map;
import lombok.Value;

@Value
public class PrecalculatedWave {
  Map<Instant, Integer> unitsBySla;
}
