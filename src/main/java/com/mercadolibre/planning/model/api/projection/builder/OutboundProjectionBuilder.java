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
import java.util.stream.Collectors;

/**
 * This class is used to project the necessary processes for outbound operation.
 */
public class OutboundProjectionBuilder implements Projector {

  static final String OUTBOUND_PROCESS_GROUP = "outbound_group";

  static final Set<ProcessName> POST_PICKING_PROCESS = Set.of(PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL, HU_ASSEMBLY, SALES_DISPATCH);

  static final Set<ProcessName> PRE_PICKING_PROCESS = Set.of(WAVING, PICKING);

  static final Set<ProcessName> OUTBOUND_PROCESSES =
      Set.of(WAVING, PICKING, PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL, HU_ASSEMBLY, SALES_DISPATCH);


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

  /**
   * Constructs context holder for the outbound operations.
   *
   * @param backlog    Backlogs which includes timestamp and path of each process.
   * @param throughput Maximum output for each processes at different timestamps.
   * @return Ordered and organized holder of contexts based on input parameters.
   */
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

    final var postPickingContext =
        buildOrderedBacklogByDateBasedProcessesContexts(postPickingBacklog, throughput, POST_PICKING_PROCESS);

    return buildBaseContextHolder(postPickingContext)
        .oneProcessContext(WAVING.getName(), wavingContext)
        .oneProcessContext(PICKING.getName(), pickingContext)
        .build();
  }

  /**
   * Transforms forecasted backlogs into a PiecewiseUpstream object.
   *
   * @param forecast The map of forecasted backlogs.
   * @return PiecewiseUpstream instance representing upstream model based on forecasted backlogs.
   */
  @Override
  public PiecewiseUpstream toUpstream(final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> forecast) {
    return buildPiecewiseUpstream(forecast);
  }

  /**
   * Calculates projected end dates based on the SLAs and provided contexts holder.
   *
   * @param slas   List of SLAs whose projected end dates are to be calculated.
   * @param holder Contexts holder based on which projected end dates are to be calculated.
   * @return SlaProjectionResult instance encapsulating the projected end dates.
   */
  @Override
  public SlaProjectionResult calculateProjectedEndDate(final List<Instant> slas, final ContextsHolder holder) {
    return ProjectorUtils.calculateProjectedEndDate(
        slas,
        holder,
        OUTBOUND_PROCESSES,
        OUTBOUND_PROCESS_GROUP);
  }

  /**
   * Computes the remaining production quantity that has not been accomplished until the SLAs cut off.
   *
   * @param updatedContext Contexts holder object updated after operations.
   * @param cutOff         Cut-off time for the SLAs.
   * @return Map of SLA instances to the corresponding remaining production quantities.
   */
  @Override
  public Map<Instant, Long> getRemainingQuantity(final ContextsHolder updatedContext, final Map<Instant, Instant> cutOff) {

    final List<String> processes = updatedContext.getProcessContextByProcessName().keySet().stream()
        .filter(s -> !s.contains(OUTBOUND_PROCESS_GROUP))
        .toList();

    return calculateRemainingQuantity(updatedContext, processes, cutOff);
  }
}
