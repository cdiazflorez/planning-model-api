package com.mercadolibre.planning.model.api.projection.waverless.idleness;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.flow.projection.tools.services.entities.context.Backlog;
import com.mercadolibre.flow.projection.tools.services.entities.context.BacklogHelper;
import com.mercadolibre.flow.projection.tools.services.entities.context.ContextsHolder;
import com.mercadolibre.flow.projection.tools.services.entities.context.DelegateAssistant;
import com.mercadolibre.flow.projection.tools.services.entities.context.Merger;
import com.mercadolibre.flow.projection.tools.services.entities.context.PiecewiseUpstream;
import com.mercadolibre.flow.projection.tools.services.entities.context.ThroughputPerHour;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDate;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDateConsumer;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.helpers.BacklogByDateHelper;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.helpers.OrderedBacklogByDateMerger;
import com.mercadolibre.flow.projection.tools.services.entities.process.ParallelProcess;
import com.mercadolibre.flow.projection.tools.services.entities.process.ParallelProcess.Context.Assistant;
import com.mercadolibre.flow.projection.tools.services.entities.process.Processor;
import com.mercadolibre.flow.projection.tools.services.entities.process.SequentialProcess;
import com.mercadolibre.flow.projection.tools.services.entities.process.SimpleProcess;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.projection.waverless.OrderedBacklogByProcessPath;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

final class BacklogProjection {

  public static final String CONSOLIDATION_PROCESS_GROUP = "consolidation_group";

  private static final String PACKING_PROCESS_GROUP = "packing_group";

  private static final List<ProcessName> OUTBOUND_PROCESSES = List.of(PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL);

  private static final Merger BACKLOG_BY_DATE_MERGER = new OrderedBacklogByDateMerger();

  private static final OrderedBacklogByDateConsumer BACKLOG_BY_DATE_CONSUMER = new OrderedBacklogByDateConsumer();

  private static final BacklogHelper BACKLOG_BY_DATE_HELPER = new BacklogByDateHelper(
      BACKLOG_BY_DATE_CONSUMER,
      BACKLOG_BY_DATE_MERGER
  );

  private static final BacklogHelper BACKLOG_BY_PROCESS_PATH_HELPER = new BacklogByDateHelper(
      new DistributionBasedConsumer(BACKLOG_BY_DATE_CONSUMER),
      new ProcessPathMerger(BACKLOG_BY_DATE_MERGER)
  );

  private static final Assistant ASSISTANT = new DelegateAssistant(
      new ProcessPathSplitter(BacklogProjection::toOrderedBacklogByDate),
      BACKLOG_BY_DATE_MERGER
  );

  private BacklogProjection() {
  }

  private static Backlog toOrderedBacklogByDate(final Map<ProcessPath, Backlog> backlogByProcessPath) {
    final var backlogs = backlogByProcessPath.values()
        .stream()
        .map(OrderedBacklogByDate.class::cast)
        .toArray(OrderedBacklogByDate[]::new);

    return BACKLOG_BY_DATE_MERGER.merge(backlogs);
  }

  /**
   * Builds context holder for a projection in which Picking's backlog is represented by an {@link OrderedBacklogByProcessPath} and the
   * rest of the processes by {@link OrderedBacklogByDate}.
   *
   * <p>Pickings' backlog is consumed by a {@link DistributionBasedConsumer} and split between the following Processes
   * taking into account the Process Path. This split backlog is (in the same splitting operation)
   * merged into an {@link OrderedBacklogByDate} representation that will be used by the following processes.
   *
   * @param backlog    current backlog of each process by process path.
   * @param throughput available processing power of each process by process path.
   * @return unconsumed context holder.
   */
  static ContextsHolder buildContexts(
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> backlog,
      final Map<ProcessName, Map<Instant, Integer>> throughput
  ) {
    final var contexts = buildOrderedBacklogByDateBasedProcessesContexts(backlog, throughput);
    final var pickingContext = buildPickingProcessContext(backlog, throughput);

    return ContextsHolder.builder()
        .oneProcessContext(PICKING.getName(), pickingContext)
        .oneProcessContext(BATCH_SORTER.getName(), contexts.get(BATCH_SORTER))
        .oneProcessContext(WALL_IN.getName(), contexts.get(WALL_IN))
        .oneProcessContext(PACKING.getName(), contexts.get(PACKING))
        .oneProcessContext(PACKING_WALL.getName(), contexts.get(PACKING_WALL))
        .oneProcessContext(PACKING_PROCESS_GROUP, new ParallelProcess.Context(ASSISTANT))
        .build();
  }

  private static Map<ProcessName, SimpleProcess.Context> buildOrderedBacklogByDateBasedProcessesContexts(
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> backlog,
      final Map<ProcessName, Map<Instant, Integer>> throughput
  ) {
    final var backlogQuantityByProcess = buildOrderedBacklogByDateBasedProcessesBacklogs(backlog);

    return OUTBOUND_PROCESSES.stream()
        .collect(
            toMap(
                Function.identity(),
                process -> new SimpleProcess.Context(
                    new ThroughputPerHour(throughput.getOrDefault(process, emptyMap())),
                    BACKLOG_BY_DATE_HELPER,
                    backlogQuantityByProcess.getOrDefault(process, OrderedBacklogByDate.emptyBacklog())
                )
            )
        );
  }

  private static Map<ProcessName, Backlog> buildOrderedBacklogByDateBasedProcessesBacklogs(
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> backlog
  ) {
    return backlog.entrySet()
        .stream()
        .filter(process -> process.getKey() != PICKING)
        .collect(
            toMap(Map.Entry::getKey, entry -> asOrderedBacklogByDate(entry.getValue().values()))
        );
  }

  private static Backlog asOrderedBacklogByDate(final Collection<Map<Instant, Long>> backlogsByDate) {
    final Map<Instant, OrderedBacklogByDate.Quantity> backlogQuantityByDate = backlogsByDate.stream()
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .collect(
            Collectors.groupingBy(
                Map.Entry::getKey,
                mapping(
                    Map.Entry::getValue,
                    collectingAndThen(reducing(0L, Long::sum), OrderedBacklogByDate.Quantity::new)
                )
            )
        );

    return new OrderedBacklogByDate(backlogQuantityByDate);
  }

  private static SimpleProcess.Context buildPickingProcessContext(
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> backlog,
      final Map<ProcessName, Map<Instant, Integer>> throughput
  ) {
    final var currentPickingBacklog = OrderedBacklogByProcessPath.from(backlog.getOrDefault(PICKING, emptyMap()));

    return new SimpleProcess.Context(
        new ThroughputPerHour(throughput.getOrDefault(PICKING, emptyMap())),
        BACKLOG_BY_PROCESS_PATH_HELPER,
        currentPickingBacklog
    );
  }

  /**
   * Builds Outbound Processes graph.
   *
   * @return graph.
   */
  static Processor buildGraph() {
    return SequentialProcess.builder()
        .name(Workflow.FBM_WMS_OUTBOUND.getName())
        .process(new SimpleProcess(PICKING.getName()))
        .process(
            ParallelProcess.builder()
                .name(PACKING_PROCESS_GROUP)
                .processor(new SimpleProcess(PACKING.getName()))
                .processor(
                    SequentialProcess.builder()
                        .name(CONSOLIDATION_PROCESS_GROUP)
                        .process(new SimpleProcess(BATCH_SORTER.getName()))
                        .process(new SimpleProcess(WALL_IN.getName()))
                        .process(new SimpleProcess(PACKING_WALL.getName()))
                        .build()
                )
                .build()
        )
        .build();
  }

  // WIP
  static ContextsHolder project(
      final Processor graph,
      final ContextsHolder holder,
      final PiecewiseUpstream upstream,
      final List<Instant> inflectionPoints
  ) {
    return graph.accept(holder, upstream, inflectionPoints);
  }
}
