package com.mercadolibre.planning.model.api.projection.builder;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.HU_ASSEMBLY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.SALES_DISPATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.projection.builder.ProjectorUtils.buildBaseContextHolder;
import static com.mercadolibre.planning.model.api.projection.builder.ProjectorUtils.buildExpeditionProcessor;
import static com.mercadolibre.planning.model.api.projection.builder.ProjectorUtils.buildOrderAssemblyProcessor;
import static com.mercadolibre.planning.model.api.projection.builder.ProjectorUtils.buildOrderedBacklogByDateBasedProcessesContexts;
import static com.mercadolibre.planning.model.api.projection.builder.ProjectorUtils.buildOrderedBacklogByProcessPathProcessContexts;
import static com.mercadolibre.planning.model.api.projection.builder.ProjectorUtils.buildPiecewiseUpstream;
import static com.mercadolibre.planning.model.api.projection.builder.ProjectorUtils.buildSimpleProcess;
import static java.util.Collections.emptyMap;

import com.mercadolibre.flow.projection.tools.services.entities.context.ContextsHolder;
import com.mercadolibre.flow.projection.tools.services.entities.context.PiecewiseUpstream;
import com.mercadolibre.flow.projection.tools.services.entities.context.ThroughputPerHour;
import com.mercadolibre.flow.projection.tools.services.entities.process.Processor;
import com.mercadolibre.flow.projection.tools.services.entities.process.SequentialProcess;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class OutboundProjectionBuilder implements Projector {

  static final String OUTBOUND_PROCESS_GROUP = "outbound_group";

  static final Set<ProcessName> PROCESS_NAMES = Set.of(PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL, HU_ASSEMBLY, SALES_DISPATCH);

  static final Set<ProcessName> PRE_PICKING_PROCESS = Set.of(WAVING, PICKING);


  /**
   * Builds Outbound projection Processes graph.
   *
   * @return graph.
   */
  @Override
  public Processor buildGraph() {
    return SequentialProcess.builder()
        .name(OUTBOUND_PROCESS_GROUP)
        .process(buildSimpleProcess(WAVING))
        .process(buildOrderAssemblyProcessor())
        .process(buildExpeditionProcessor())
        .build();
  }

  @Override
  public ContextsHolder buildContextHolder(final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> backlog,
                                           final Map<ProcessName, Map<Instant, Integer>> throughput) {
    final var wavingThroughput = new InstantThroughput(throughput.getOrDefault(WAVING, emptyMap()));
    final var wavingContext = buildOrderedBacklogByProcessPathProcessContexts(backlog, WAVING, wavingThroughput);

    final var pickingThroughput = new ThroughputPerHour(throughput.getOrDefault(PICKING, emptyMap()));
    final var pickingContext = buildOrderedBacklogByProcessPathProcessContexts(backlog, PICKING, pickingThroughput);

    final var postPickingBacklog = backlog.entrySet().stream()
        .filter(entry -> !PRE_PICKING_PROCESS.contains(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    final var postPickingContext = buildOrderedBacklogByDateBasedProcessesContexts(postPickingBacklog, throughput, PROCESS_NAMES);

    return buildBaseContextHolder(postPickingContext)
        .oneProcessContext(WAVING.getName(), wavingContext)
        .oneProcessContext(PICKING.getName(), pickingContext)
        .build();
  }

  @Override
  public PiecewiseUpstream toUpstream(final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> forecast) {
    return buildPiecewiseUpstream(forecast);
  }

  @Override
  public SlaProjectionResult calculateProjectedEndDate(List<Instant> slas, ContextsHolder holder) {
    return new SlaProjectionResult(List.of());
  }

  @Override
  public Map<Instant, Long> getRemainingQuantity(ContextsHolder updatedContext, Map<Instant, Instant> cutOff) {
    return Map.of();
  }
}
