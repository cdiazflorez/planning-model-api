package com.mercadolibre.planning.model.api.projection.builder;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.projection.builder.ProjectorUtils.buildBaseContextHolder;
import static com.mercadolibre.planning.model.api.projection.builder.ProjectorUtils.buildOrderAssemblyProcessor;
import static com.mercadolibre.planning.model.api.projection.builder.ProjectorUtils.buildOrderedBacklogByDateBasedProcessesContexts;
import static com.mercadolibre.planning.model.api.projection.builder.ProjectorUtils.buildOrderedBacklogByProcessPathProcessContexts;
import static com.mercadolibre.planning.model.api.projection.builder.ProjectorUtils.buildPiecewiseUpstream;
import static com.mercadolibre.planning.model.api.projection.builder.ProjectorUtils.buildSimpleProcess;
import static com.mercadolibre.planning.model.api.projection.builder.ProjectorUtils.calculateRemainingQuantity;
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


public class FromWavingToPackingProjectionBuilder implements Projector {

  static final String OUTBOUND_PROCESS_GROUP = "pre_expedition_group";
  static final Set<ProcessName> PROCESS_NAMES = Set.of(PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL);
  private static final Set<ProcessName> PRE_EXPEDITION_PROCESSES = Set.of(WAVING, PICKING, PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL);


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
        .build();
  }

  @Override
  public ContextsHolder buildContextHolder(final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> backlog,
                                           final Map<ProcessName, Map<Instant, Integer>> throughput) {
    final var wavingThroughput = new InstantThroughput(throughput.getOrDefault(WAVING, emptyMap()));
    final var wavingContext = buildOrderedBacklogByProcessPathProcessContexts(backlog, WAVING, wavingThroughput);

    final var pickingThroughput = new ThroughputPerHour(throughput.getOrDefault(PICKING, emptyMap()));
    final var pickingContext = buildOrderedBacklogByProcessPathProcessContexts(backlog, PICKING, pickingThroughput);

    final var contexts = buildOrderedBacklogByDateBasedProcessesContexts(backlog, throughput, PROCESS_NAMES);

    return buildBaseContextHolder(contexts)
        .oneProcessContext(WAVING.getName(), wavingContext)
        .oneProcessContext(PICKING.getName(), pickingContext)
        .build();
  }

  @Override
  public PiecewiseUpstream toUpstream(final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> forecast) {
    return buildPiecewiseUpstream(forecast);
  }

  /**
   * Calculates the projected end date for each provided Service Level Agreement (SLA). The projected end date
   * is the date when the last unit of each SLA will be processed by the final process.
   *
   * @param slas     The list of SLAs for which a projected end date will be calculated.
   * @param contexts The ContextHolder from which to extract the projection results.
   * @return A new SlaProjectionResult containing the SLA projection results for each provided SLA.
   */
  @Override
  public SlaProjectionResult calculateProjectedEndDate(final List<Instant> slas, final ContextsHolder contexts) {

    return ProjectorUtils.calculateProjectedEndDate(
        slas,
        contexts,
        PRE_EXPEDITION_PROCESSES,
        OUTBOUND_PROCESS_GROUP);
  }

  /**
   * Returns the remaining quantity of a product, given the updated context and cut-off times.
   * The processes present in the updated ContextHolder are filtered to exclude those that contain
   * values of OUTBOUND_PROCESS_GROUP, PACKING_PROCESS_GROUP, WAVING name, and PICKING name.
   * Next, it gets the processed and unprocessed backlog states for wave and picking processes.
   * Finally, it uses ProjectorUtils.getRemainingQuantity to calculate and return the remaining quantity.
   *
   * @param updatedContext is the updated ContextHolder with the latest data.
   * @param cutOff         is a Map of Instant to Instant indicating the cut-off times.
   * @return a Map of Instant to Long indicating the remaining quantity for each given Instant in 'cutOff'.
   */
  @Override
  public Map<Instant, Long> getRemainingQuantity(final ContextsHolder updatedContext, final Map<Instant, Instant> cutOff) {

    final Set<String> processes = updatedContext.getProcessContextByProcessName().keySet();

    final List<String> finalProcesses = processes.stream()
        .filter(s -> !s.contains(OUTBOUND_PROCESS_GROUP))
        .toList();

    return calculateRemainingQuantity(updatedContext, finalProcesses, cutOff);
  }

}
