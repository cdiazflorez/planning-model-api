package com.mercadolibre.planning.model.api.projection.builder;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static java.time.temporal.ChronoUnit.HOURS;
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
import com.mercadolibre.flow.projection.tools.services.entities.context.ProcessedBacklogState;
import com.mercadolibre.flow.projection.tools.services.entities.context.ThroughputPerHour;
import com.mercadolibre.flow.projection.tools.services.entities.context.UnprocessedBacklogState;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDate;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDateConsumer;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.helpers.BacklogByDateHelper;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.helpers.OrderedBacklogByDateMerger;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.utils.OrderedBacklogByDateUtils;
import com.mercadolibre.flow.projection.tools.services.entities.process.ParallelProcess;
import com.mercadolibre.flow.projection.tools.services.entities.process.Processor;
import com.mercadolibre.flow.projection.tools.services.entities.process.SequentialProcess;
import com.mercadolibre.flow.projection.tools.services.entities.process.SimpleProcess;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.BacklogProjection;
import com.mercadolibre.planning.model.api.projection.backlogmanager.DistributionBasedConsumer;
import com.mercadolibre.planning.model.api.projection.backlogmanager.OrderedBacklogByProcessPath;
import com.mercadolibre.planning.model.api.projection.backlogmanager.ProcessPathMerger;
import com.mercadolibre.planning.model.api.projection.waverless.idleness.ProcessPathSplitter;
import com.mercadolibre.planning.model.api.util.DateUtils;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * PackingProjectionBuilder provides the methods to build and extract the results from a projection that takes into account the
 * processes from picking up to packing and packing wall.
 */
public class PackingProjectionBuilder implements Projector {

  private static final String GLOBAL_PROCESS = "order_assembly";

  private static final String CONSOLIDATION_PROCESS_GROUP = "consolidation_group";

  private static final String PACKING_PROCESS_GROUP = "packing_group";

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

  private static final ParallelProcess.Context.Assistant ASSISTANT = new DelegateAssistant(
      new ProcessPathSplitter(BacklogProjection::toOrderedBacklogByDate),
      BACKLOG_BY_DATE_MERGER
  );

  private static final Set<ProcessName> POST_PICKING_PROCESSES = Set.of(PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL);

  private static Map<ProcessName, SimpleProcess.Context> buildOrderedBacklogByDateBasedProcessesContexts(
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> backlog,
      final Map<ProcessName, Map<Instant, Integer>> throughput
  ) {
    final var backlogQuantityByProcess = buildOrderedBacklogByDateBasedProcessesBacklogs(backlog);

    return POST_PICKING_PROCESSES.stream()
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
  @Override
  public Processor buildGraph() {
    return SequentialProcess.builder()
        .name(GLOBAL_PROCESS)
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

  /**
   * Maps a forecast by date_in, process_path and date_out as a PiecewiseUpstream of {@link OrderedBacklogByProcessPath}.
   * This PiecewiseUpstream assumes that the forecasted values are represent a whole hour.
   *
   * @param forecastedBacklog sales forecast.
   * @return sales forecast as Piecewise Upstream.
   */
  @Override
  public PiecewiseUpstream toUpstream(final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> forecastedBacklog) {
    if (forecastedBacklog.isEmpty()) {
      return new PiecewiseUpstream(emptyMap());
    }

    final var firstHour = Collections.min(forecastedBacklog.keySet());
    final var lastHour = Collections.max(forecastedBacklog.keySet());

    final var upstreamByHour = DateUtils.instantRange(firstHour, lastHour.plus(1L, HOURS), HOURS)
        .collect(
            toMap(
                Function.identity(),
                date -> OrderedBacklogByProcessPath.from(
                    forecastedBacklog.getOrDefault(date, emptyMap())
                )
            )
        );

    return new PiecewiseUpstream(upstreamByHour);
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
        .map(sla -> new SlaProjectionResult.Sla(sla, projectedEndDates.get(sla), 0D))
        .collect(Collectors.toList());

    return new SlaProjectionResult(results);
  }

}
