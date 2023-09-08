package com.mercadolibre.planning.model.api.projection;

import static com.mercadolibre.planning.model.api.util.DateUtils.generateInflectionPoints;

import com.mercadolibre.flow.projection.tools.services.entities.context.ContextsHolder;
import com.mercadolibre.flow.projection.tools.services.entities.context.Upstream;
import com.mercadolibre.flow.projection.tools.services.entities.process.Processor;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.builder.Projector;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Projection {
  private static final int INFLECTION_WINDOW_SIZE_IN_MINUTES = 5;

  private Projection() {
  }

  public static ContextsHolder execute(
      final Instant executionDateFrom,
      final Instant executionDateTo,
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentBacklog,
      final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> forecastBacklog,
      final Map<ProcessName, Map<Instant, Integer>> throughput,
      final Projector projector
  ) {
    final List<Instant> inflectionPoints = generateInflectionPoints(executionDateFrom, executionDateTo, INFLECTION_WINDOW_SIZE_IN_MINUTES);

    final Processor graph = projector.buildGraph();

    final ContextsHolder contexts = projector.buildContextHolder(currentBacklog, throughput);

    final Upstream upstream = projector.toUpstream(forecastBacklog);

    return graph.accept(contexts, upstream, inflectionPoints);
  }
}
