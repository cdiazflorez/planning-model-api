package com.mercadolibre.planning.model.api.projection.waverless;


import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import java.time.Instant;
import java.util.Map;
import lombok.Value;

@Value
public class BacklogLimits {
  Map<ProcessName, Map<Instant, Integer>> lower;

  Map<ProcessName, Map<Instant, Integer>> upper;
}
