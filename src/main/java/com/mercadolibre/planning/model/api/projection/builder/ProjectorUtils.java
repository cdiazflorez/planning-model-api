package com.mercadolibre.planning.model.api.projection.builder;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.EXPEDITION;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.HU_ASSEMBLY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.SALES_DISPATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.util.DateUtils.instantRange;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.flow.projection.tools.services.entities.context.Backlog;
import com.mercadolibre.flow.projection.tools.services.entities.context.BacklogHelper;
import com.mercadolibre.flow.projection.tools.services.entities.context.BacklogState;
import com.mercadolibre.flow.projection.tools.services.entities.context.ContextsHolder;
import com.mercadolibre.flow.projection.tools.services.entities.context.DelegateAssistant;
import com.mercadolibre.flow.projection.tools.services.entities.context.Merger;
import com.mercadolibre.flow.projection.tools.services.entities.context.PiecewiseUpstream;
import com.mercadolibre.flow.projection.tools.services.entities.context.ProcessContext;
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
 * A utility class with methods for managing and calculating ProjectionBuilders processes for workflow.
 */
final class ProjectorUtils {

  private static final OrderedBacklogByDateConsumer BACKLOG_BY_DATE_CONSUMER = new OrderedBacklogByDateConsumer();
  private static final Merger BACKLOG_BY_DATE_MERGER = new OrderedBacklogByDateMerger();
  static final ParallelProcess.Context.Assistant ASSISTANT = new DelegateAssistant(
      new ProcessPathSplitter(BacklogProjection::toOrderedBacklogByDate),
      BACKLOG_BY_DATE_MERGER
  );
  private static final BacklogHelper BACKLOG_BY_DATE_HELPER = new BacklogByDateHelper(
      BACKLOG_BY_DATE_CONSUMER,
      BACKLOG_BY_DATE_MERGER
  );
  private static final String CONSOLIDATION_PROCESS_GROUP = "consolidation_group";
  private static final double MIN_THROUGHPUT_PERCENTAGE = 0.05;
  static final BacklogHelper BACKLOG_BY_PROCESS_PATH_HELPER = new BacklogByDateHelper(
      new DistributionBasedConsumer(BACKLOG_BY_DATE_CONSUMER, MIN_THROUGHPUT_PERCENTAGE),
      new ProcessPathMerger(BACKLOG_BY_DATE_MERGER)
  );
  private static final String ORDER_ASSEMBLY_PROCESS_GROUP = "order_assembly";
  private static final String PACKING_PROCESS_GROUP = "packing_group";
  private static final Set<ProcessName> PRE_PACKING_PROCESSES = Set.of(WAVING, PICKING);

  private ProjectorUtils() {
  }

  /**
   * Constructs contexts for processes using ordered backlogs by date.
   *
   * @param backlog      Map of process names to a map of process paths by timestamps.
   * @param throughput   Map of a process to its throughput at different timestamps.
   * @param processNames Set of process names to construct contexts for.
   * @return Map of process names to their corresponding contexts.
   */
  static Map<ProcessName, SimpleProcess.Context> buildOrderedBacklogByDateBasedProcessesContexts(
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> backlog,
      final Map<ProcessName, Map<Instant, Integer>> throughput,
      final Set<ProcessName> processNames
  ) {
    final var backlogQuantityByProcess = buildOrderedBacklogProcess(backlog);

    return processNames.stream()
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

  private static Map<ProcessName, Backlog> buildOrderedBacklogProcess(
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> backlog
  ) {
    return backlog.entrySet().stream()
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

  /**
   * Builds an instance of OrderedBacklogByProcessPath from a map of process paths to maps of instants to longs.
   *
   * @param backlog          The map of backlog to convert.
   * @param processName      The name of the process.
   * @param throughputGetter A function to retrieve the throughput of the process.
   * @return A SimpleProcess.Context instance representing the context of the process.
   */
  static SimpleProcess.Context buildOrderedBacklogByProcessPathProcessContexts(
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> backlog,
      final ProcessName processName,
      final SimpleProcess.Throughput throughputGetter
  ) {
    final var currentBacklog = OrderedBacklogByProcessPath.from(backlog.getOrDefault(processName, emptyMap()));

    return new SimpleProcess.Context(
        throughputGetter,
        BACKLOG_BY_PROCESS_PATH_HELPER,
        currentBacklog
    );
  }

  /**
   * Builds an instance of ContextsHolder using a map of process names to contexts.
   *
   * @param contextMap The map of process names to contexts.
   * @return A ContextsHolder instance representing the context holder of the process map.
   */
  static ContextsHolder.ContextsHolderBuilder buildBaseContextHolder(
      final Map<ProcessName, SimpleProcess.Context> contextMap
  ) {
    final ContextsHolder.ContextsHolderBuilder builder = ContextsHolder.builder();
    contextMap.forEach((process, context) -> builder.oneProcessContext(process.getName(), context));

    return builder.oneProcessContext(PACKING_PROCESS_GROUP, new ParallelProcess.Context(ASSISTANT));
  }

  public static SimpleProcess buildSimpleProcess(final ProcessName processName) {
    return new SimpleProcess(processName.getName());
  }

  public static Processor buildOrderAssemblyProcessor() {
    return SequentialProcess.builder()
        .name(ORDER_ASSEMBLY_PROCESS_GROUP)
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

  public static Processor buildExpeditionProcessor() {
    return SequentialProcess.builder()
        .name(EXPEDITION.getName())
        .process(new SimpleProcess(HU_ASSEMBLY.getName()))
        .process(new SimpleProcess(SALES_DISPATCH.getName()))
        .build();
  }

  /**
   * Builds a PiecewiseUpstream instance from a forecasted backlog.
   *
   * @param forecastedBacklog The forecasted backlog in a map data structure.
   * @return The desired PiecewiseUpstream instance.
   */
  public static PiecewiseUpstream buildPiecewiseUpstream(final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> forecastedBacklog) {
    if (forecastedBacklog.isEmpty()) {
      return new PiecewiseUpstream(emptyMap());
    }

    final var firstHour = Collections.min(forecastedBacklog.keySet());
    final var lastHour = Collections.max(forecastedBacklog.keySet());

    final var upstreamByHour = instantRange(firstHour, lastHour.plus(1L, HOURS), HOURS)
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

  public static Stream<OrderedBacklogByDate> getProcessBacklogs(final ContextsHolder contexts, final Set<ProcessName> processes) {
    return processes.stream()
        .map(ProcessName::getName)
        .map(contexts::getProcessContextByProcessName)
        .map(SimpleProcess.Context.class::cast)
        .map(SimpleProcess.Context::getLastUnprocessedBacklogState)
        .map(UnprocessedBacklogState::getBacklog)
        .map(OrderedBacklogByDate.class::cast);
  }

  public static Stream<OrderedBacklogByDate> getOrderedBacklogByDate(final ContextsHolder contexts, final String processName) {

    final var processContext = (SimpleProcess.Context) contexts.getProcessContextByProcessName()
        .get(processName);

    var lastBacklog = (OrderedBacklogByProcessPath) processContext.getLastUnprocessedBacklogState()
        .getBacklog();

    return lastBacklog.getBacklogs()
        .values()
        .stream()
        .map(OrderedBacklogByDate.class::cast);
  }

  public static List<OrderedBacklogByDate> getGlobalProcessedBacklogs(final ContextsHolder contexts, final String globalContextProcess) {
    return contexts.getProcessContextByProcessName(globalContextProcess)
        .getProcessedBacklog()
        .stream()
        .map(ProcessedBacklogState::getBacklog)
        .map(OrderedBacklogByDate.class::cast)
        .toList();
  }

  /**
   * Calculates the projected end date based on SLAs, context holder, final processes, and a global context process.
   *
   * @param slas                 The list of SLA instances.
   * @param contexts             Contains the context holder object.
   * @param finalProcesses       Set of final process names.
   * @param globalContextProcess Global context process name as a string.
   * @return An instance of SlaProjectionResult, encapsulating the projected end dates.
   */
  public static SlaProjectionResult calculateProjectedEndDate(
      final List<Instant> slas,
      final ContextsHolder contexts,
      final Set<ProcessName> finalProcesses,
      final String globalContextProcess) {

    final Set<ProcessName> postPickingProcesses = finalProcesses.stream()
        .filter(processName -> !PRE_PACKING_PROCESSES.contains(processName))
        .collect(Collectors.toSet());

    final Stream<OrderedBacklogByDate> prePacking = finalProcesses.stream()
        .filter(PRE_PACKING_PROCESSES::contains)
        .flatMap(processName -> getOrderedBacklogByDate(contexts, processName.getName()));

    final Stream<OrderedBacklogByDate> postPickingProcessesBacklogs = getProcessBacklogs(contexts, postPickingProcesses);

    final List<OrderedBacklogByDate> simpleProcessLastBacklogs = Stream.concat(prePacking, postPickingProcessesBacklogs).toList();

    final List<OrderedBacklogByDate> globalProcessedBacklogs = getGlobalProcessedBacklogs(contexts, globalContextProcess);

    final Map<Instant, Instant> projectedEndDates = OrderedBacklogByDateUtils.calculateProjectedEndDate(
        simpleProcessLastBacklogs,
        globalProcessedBacklogs,
        slas
    );

    final List<SlaProjectionResult.Sla> results = slas.stream()
        .map(sla -> new SlaProjectionResult.Sla(sla, projectedEndDates.get(sla), 0D))
        .toList();

    return new SlaProjectionResult(results);
  }

  /**
   * To get remaining quantity
   *
   * @param updatedContext a context holder updated after projection
   * @param processes      Set of final process names.
   * @param cutOff         sla's cut-off timestamps.
   * @return Returns a Map with remaining quantities by SLA instances.
   */
  static Map<Instant, Long> calculateRemainingQuantity(
      final ContextsHolder updatedContext,
      final List<String> processes,
      final Map<Instant, Instant> cutOff
  ) {
    final List<BacklogState> processed = createBacklogStream(processes, updatedContext, true).toList();

    final List<BacklogState> unprocessed = createBacklogStream(processes, updatedContext, false).toList();

    return OrderedBacklogByDateUtils.getRemainingQuantity(processed, unprocessed, cutOff);
  }

  private static Stream<BacklogState> createBacklogStream(
      final List<String> nextProcesses,
      final ContextsHolder updatedContext,
      final boolean isProcessed
  ) {

    return nextProcesses.stream()
        .filter(processName -> updatedContext.getProcessContextByProcessName(processName) instanceof SimpleProcess.Context)
        .map(updatedContext::getProcessContextByProcessName)
        .flatMap(entry -> createBacklogStateStream(entry, isProcessed));
  }

  private static Stream<BacklogState> createBacklogStateStream(
      final ProcessContext processContext,
      final boolean isProcessed
  ) {
    final SimpleProcess.Context context = (SimpleProcess.Context) processContext;
    final Stream<? extends BacklogState> backlogStream = isProcessed
        ? context.getProcessedBacklog().stream()
        : context.getUnprocessedBacklog().stream();

    return backlogStream
        .flatMap(ProjectorUtils::processBacklogState);
  }

  private static Stream<BacklogState> processBacklogState(final BacklogState backlogState) {
    if (backlogState.getBacklog() instanceof OrderedBacklogByProcessPath backlog) {
      return backlog.getBacklogs().values().stream()
          .map(OrderedBacklogByDate.class::cast)
          .map(orderedBacklogByDate -> new ProcessedBacklogState(
              backlogState.getStartDate(),
              backlogState.getEndDate(),
              orderedBacklogByDate));
    }
    return Stream.of(backlogState);
  }

}
