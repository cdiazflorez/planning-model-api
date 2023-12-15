package com.mercadolibre.planning.model.api.projection.builder;

import com.mercadolibre.flow.projection.tools.services.entities.process.SimpleProcess;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InstantThroughput implements SimpleProcess.Throughput {
  final Map<Instant, Integer> throughputs;

  @Override
  public int availableBetween(final Instant dateFrom, final Instant dateTo) {
    return throughputs.getOrDefault(dateFrom, 0);
  }
}
