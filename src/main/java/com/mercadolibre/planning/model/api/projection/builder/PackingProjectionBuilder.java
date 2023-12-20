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
import static com.mercadolibre.planning.model.api.projection.builder.ProjectorUtils.buildPiecewiseUpstream;
import static com.mercadolibre.planning.model.api.projection.builder.SlaProjectionResult.Sla;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.flow.projection.tools.services.entities.context.BacklogHelper;
import com.mercadolibre.flow.projection.tools.services.entities.context.BacklogState;
import com.mercadolibre.flow.projection.tools.services.entities.context.ContextsHolder;
import com.mercadolibre.flow.projection.tools.services.entities.context.Merger;
import com.mercadolibre.flow.projection.tools.services.entities.context.PiecewiseUpstream;
import com.mercadolibre.flow.projection.tools.services.entities.context.ProcessedBacklogState;
import com.mercadolibre.flow.projection.tools.services.entities.context.ThroughputPerHour;
import com.mercadolibre.flow.projection.tools.services.entities.context.UnprocessedBacklogState;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDate;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDateConsumer;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.helpers.BacklogByDateHelper;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.helpers.OrderedBacklogByDateMerger;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.utils.OrderedBacklogByDateUtils;
import com.mercadolibre.flow.projection.tools.services.entities.process.Processor;
import com.mercadolibre.flow.projection.tools.services.entities.process.SimpleProcess;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.backlogmanager.DistributionBasedConsumer;
import com.mercadolibre.planning.model.api.projection.backlogmanager.OrderedBacklogByProcessPath;
import com.mercadolibre.planning.model.api.projection.backlogmanager.ProcessPathMerger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * PackingProjectionBuilder provides the methods to build and extract the results from a projection that takes into account the
 * processes from picking up to packing and packing wall.
 */
public class PackingProjectionBuilder implements Projector {

  private static final String GLOBAL_PROCESS = "order_assembly";

  private static final String PACKING_PROCESS_GROUP = "packing_group";

  private static final Set<ProcessName> PROCESS_NAME = Set.of(PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL);

  private static final Merger BACKLOG_BY_DATE_MERGER = new OrderedBacklogByDateMerger();

  private static final OrderedBacklogByDateConsumer BACKLOG_BY_DATE_CONSUMER = new OrderedBacklogByDateConsumer();

  private static final double MIN_THROUGHPUT_PERCENTAGE = 0.05;

  private static final BacklogHelper BACKLOG_BY_PROCESS_PATH_HELPER = new BacklogByDateHelper(
      new DistributionBasedConsumer(BACKLOG_BY_DATE_CONSUMER, MIN_THROUGHPUT_PERCENTAGE),
      new ProcessPathMerger(BACKLOG_BY_DATE_MERGER)
  );


  private static final Set<ProcessName> POST_PICKING_PROCESSES = Set.of(PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL);

  private static SimpleProcess.Context buildPickingProcessContext(
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> backlog,
      final Map<ProcessName, Map<Instant, Integer>> throughput
  ) {
    final Map<ProcessPath, Map<Instant, Long>> pickingWithWavingBacklog = Stream.concat(
        backlog.getOrDefault(WAVING, emptyMap()).entrySet().stream(),
        backlog.getOrDefault(PICKING, emptyMap()).entrySet().stream()
    ).collect(
        groupingBy(
            Map.Entry::getKey,
            flatMapping(
                entry -> entry.getValue()
                    .entrySet()
                    .stream(),
                toMap(Map.Entry::getKey, Map.Entry::getValue, Long::sum)
            )
        )
    );

    final var currentPickingBacklog = OrderedBacklogByProcessPath.from(pickingWithWavingBacklog);

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
  @Override
  public Processor buildGraph() {
    return buildOrderAssemblyProcessor();
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
  @Override
  public ContextsHolder buildContextHolder(
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> backlog,
      final Map<ProcessName, Map<Instant, Integer>> throughput
  ) {
      final var contexts = buildOrderedBacklogByDateBasedProcessesContexts(backlog, throughput, PROCESS_NAME);
      final var pickingContext = buildPickingProcessContext(backlog, throughput);

      return buildBaseContextHolder(contexts)
              .oneProcessContext(PICKING.getName(), pickingContext)
              .build();
  }

  /**
   * Maps a forecast by date_in, process_path and date_out as a PiecewiseUpstream of {@link OrderedBacklogByProcessPath}.
   * This PiecewiseUpstream assumes that the forecasted values are represent a whole hour.
   *
   * @param forecastedBacklog sales forecast.
   * @return sales forecast as Piecewise Upstream.
   */
  @Override
  public PiecewiseUpstream toUpstream(final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> forecastedBacklog) {
    return buildPiecewiseUpstream(forecastedBacklog);
  }

  /**
   * Given an SLA list and a ContextHolder that is the result of a projection, extract the projected en date for each SLA. The projected
   * end date is the date in which the last unit of each SLA will be processed by the last process. If any unit for an SLA remain in an
   * unprocessed backlog state then, it can not be assigned a projected end date for that SLA and will result in a null value.
   *
   * @param slas     for which a projected end date will be calculated.
   * @param contexts ContextHolder from which to extract the projection results.
   * @return SlaProjectionResult a wrapper around the projected values for each SLA.
   */
  @Override
  public SlaProjectionResult calculateProjectedEndDate(final List<Instant> slas, final ContextsHolder contexts) {

    final var postPickingProcessesBacklogs = POST_PICKING_PROCESSES.stream()
        .map(ProcessName::getName)
        .map(contexts::getProcessContextByProcessName)
        .map(SimpleProcess.Context.class::cast)
        .map(SimpleProcess.Context::getLastUnprocessedBacklogState)
        .map(UnprocessedBacklogState::getBacklog)
        .map(OrderedBacklogByDate.class::cast);

    final var picking = (SimpleProcess.Context) contexts.getProcessContextByProcessName()
        .get(PICKING.getName());

    final var pickingLastBacklog = (OrderedBacklogByProcessPath) picking.getLastUnprocessedBacklogState()
        .getBacklog();

    final var pickingOrderedBacklogByDate = pickingLastBacklog.getBacklogs()
        .values()
        .stream()
        .map(OrderedBacklogByDate.class::cast);

    final var simpleProcessLastBacklogs = Stream.concat(pickingOrderedBacklogByDate, postPickingProcessesBacklogs)
        .collect(Collectors.toList());

    final var globalProcessedBacklogs = contexts.getProcessContextByProcessName(GLOBAL_PROCESS)
        .getProcessedBacklog()
        .stream()
        .map(ProcessedBacklogState::getBacklog)
        .map(OrderedBacklogByDate.class::cast)
        .collect(Collectors.toList());


    final var projectedEndDates = OrderedBacklogByDateUtils.calculateProjectedEndDate(
        simpleProcessLastBacklogs,
        globalProcessedBacklogs,
        slas
    );

    final var results = slas.stream()
        .map(sla -> new Sla(sla, projectedEndDates.get(sla), 0D))
        .collect(Collectors.toList());

    return new SlaProjectionResult(results);
  }

  @Override
  public Map<Instant, Long> getRemainingQuantity(final ContextsHolder updatedContext, final Map<Instant, Instant> cutOff) {
    final Set<String> processes = updatedContext.getProcessContextByProcessName().keySet();

    final List<String> finalProcesses = processes.stream()
        .filter(s -> !s.contains(PACKING_PROCESS_GROUP) && !s.contains(GLOBAL_PROCESS) && !s.contains(PICKING.getName()))
        .toList();

    final SimpleProcess.Context picking = (SimpleProcess.Context) updatedContext.getProcessContextByProcessName().get(PICKING.getName());

    final Stream<ProcessedBacklogState> pickingProcessedBacklogStates = picking.getProcessedBacklog().stream()
        .flatMap(processedBacklogState -> {
          final OrderedBacklogByProcessPath backlog = (OrderedBacklogByProcessPath) processedBacklogState.getBacklog();
          return backlog.getBacklogs().values().stream()
              .map(OrderedBacklogByDate.class::cast)
              .map(orderedBacklogByDate -> new ProcessedBacklogState(
                  processedBacklogState.getStartDate(),
                  processedBacklogState.getEndDate(),
                  orderedBacklogByDate));
        });

    final Stream<UnprocessedBacklogState> pickingUnprocessedBacklogStates = picking.getUnprocessedBacklog().stream()
        .flatMap(processedBacklogState -> {
          final OrderedBacklogByProcessPath backlog = (OrderedBacklogByProcessPath) processedBacklogState.getBacklog();
          return backlog.getBacklogs().values().stream()
              .map(OrderedBacklogByDate.class::cast)
              .map(orderedBacklogByDate -> new UnprocessedBacklogState(
                  processedBacklogState.getStartDate(),
                  processedBacklogState.getEndDate(),
                  orderedBacklogByDate));
        });

    final Stream<BacklogState> processed = finalProcesses.stream()
        .map(updatedContext::getProcessContextByProcessName)
        .map(SimpleProcess.Context.class::cast)
        .map(SimpleProcess.Context::getProcessedBacklog)
        .flatMap(List::stream)
        .map(BacklogState.class::cast);

    final Stream<BacklogState> unprocessed = finalProcesses.stream()
        .map(updatedContext::getProcessContextByProcessName)
        .map(SimpleProcess.Context.class::cast)
        .map(SimpleProcess.Context::getUnprocessedBacklog)
        .flatMap(List::stream)
        .map(BacklogState.class::cast);

    final List<BacklogState> finalProcessed = Stream.concat(processed, pickingProcessedBacklogStates).toList();

    final List<BacklogState> finalUnprocessed = Stream.concat(unprocessed, pickingUnprocessedBacklogStates).toList();

    return OrderedBacklogByDateUtils.getRemainingQuantity(finalProcessed, finalUnprocessed, cutOff);
  }

}
