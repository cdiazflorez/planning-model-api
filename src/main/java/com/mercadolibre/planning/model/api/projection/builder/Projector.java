package com.mercadolibre.planning.model.api.projection.builder;

import com.mercadolibre.flow.projection.tools.services.entities.context.ContextsHolder;
import com.mercadolibre.flow.projection.tools.services.entities.context.Upstream;
import com.mercadolibre.flow.projection.tools.services.entities.process.Processor;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Provides the methods to build and extract the results from a projection.
 */
public interface Projector {
  Processor buildGraph();

  ContextsHolder buildContextHolder(
      Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> backlog,
      Map<ProcessName, Map<Instant, Integer>> throughput
  );

  Upstream toUpstream(Map<Instant, Map<ProcessPath, Map<Instant, Long>>> forecast);

  SlaProjectionResult calculateProjectedEndDate(List<Instant> slas, ContextsHolder holder);

}
