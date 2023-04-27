package com.mercadolibre.planning.model.api.projection.waverless;

import static com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.utils.OrderedBacklogByDateUtils.calculateProjectedEndDate;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.mercadolibre.flow.projection.tools.services.entities.context.ContextsHolder;
import com.mercadolibre.flow.projection.tools.services.entities.context.DelegateAssistant;
import com.mercadolibre.flow.projection.tools.services.entities.context.PiecewiseUpstream;
import com.mercadolibre.flow.projection.tools.services.entities.context.ProcessContext;
import com.mercadolibre.flow.projection.tools.services.entities.context.ThroughputPerHour;
import com.mercadolibre.flow.projection.tools.services.entities.context.UnprocessedBacklogState;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDate;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDate.Quantity;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDateConsumer;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.helpers.BacklogByDateHelper;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.helpers.OrderedBacklogByDateMerger;
import com.mercadolibre.flow.projection.tools.services.entities.process.ParallelProcess;
import com.mercadolibre.flow.projection.tools.services.entities.process.Processor;
import com.mercadolibre.flow.projection.tools.services.entities.process.SimpleProcess;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Value;

/**
 * Builds graph and context for projections that only consider picking by Process Path.
 */
public final class PickingProjectionBuilder {

  private static final String MISSING_PROCESS_PATHS_ERROR_MESSAGE = "Process Paths must not be null or empty";

  private static final String PICKING_PROCESS = "picking";

  private static final BacklogByDateHelper HELPER = new BacklogByDateHelper(
      new OrderedBacklogByDateConsumer(),
      new OrderedBacklogByDateMerger()
  );

  private static final OrderedBacklogByDateMerger MERGER = new OrderedBacklogByDateMerger();

  private PickingProjectionBuilder() {
  }

  public static Processor buildGraph(final List<ProcessPath> processPaths) {
    if (isEmpty(processPaths)) {
      throw new IllegalArgumentException(MISSING_PROCESS_PATHS_ERROR_MESSAGE);
    }

    final List<Processor> processors = processPaths.stream()
        .map(PickingProjectionBuilder::processorName)
        .map(SimpleProcess::new)
        .collect(Collectors.toList());

    return new ParallelProcess(PICKING_PROCESS, processors);
  }

  public static ContextsHolder buildContextHolder(
      final Map<ProcessPath, Map<Instant, Long>> currentBacklog,
      final Map<ProcessPath, Map<Instant, Integer>> throughput
  ) {
    final var processPaths = throughput.keySet();

    final Map<String, ProcessContext> contexts = new HashMap<>();
    contexts.put(PICKING_PROCESS, new ParallelProcess.Context(buildParallelAssistant(processPaths)));
    contexts.putAll(buildSimpleProcessContexts(currentBacklog, throughput));

    return new ContextsHolder(contexts);
  }

  /**
   * SLA projection for Picking by Process Path.
   *
   * @param processor        graph that will be executed.
   * @param contexts         parameters of the processes that belong to the graph that will be executed.
   * @param waves            upstream backlog to feed the first process by ingestion date, process path and sla
   * @param inflectionPoints points in time for which the projection will be evaluated
   * @param processPaths     that compose the graph, for which a result is required
   * @param slas             SLAs to query the graph
   * @return projected end date by process path and sla
   */
  public static Map<ProcessPath, Map<Instant, Instant>> projectSla(
      final Processor processor,
      final ContextsHolder contexts,
      final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> waves,
      final List<Instant> inflectionPoints,
      final List<ProcessPath> processPaths,
      final List<Instant> slas
  ) {
    final var updatedContexts = processor.accept(
        contexts, ProjectionUtils.asPiecewiseUpstream(waves), inflectionPoints
    );

    return processPaths.stream()
        .collect(toMap(Function.identity(), path -> getProjectedEndDate(path, updatedContexts, slas)));
  }

  private static Map<Instant, Instant> getProjectedEndDate(
      final ProcessPath processPath,
      final ContextsHolder holder,
      final List<Instant> slas
  ) {
    final var key = processorName(processPath);
    final var processPathContext = holder.getProcessContextByProcessName(key);

    return calculateProjectedEndDate(new ContextsHolder(Map.of(key, processPathContext)), key, slas);
  }

  private static String processorName(final ProcessPath path) {
    return path.toString();
  }


  public static List<ProcessPathBacklog> backlogProjection(
      final Processor process,
      final ContextsHolder contexts,
      final PiecewiseUpstream upstream,
      final List<Instant> inflectionPoints,
      final Set<ProcessPath> processPaths) {

    final var processedContexts = process.accept(contexts, upstream, inflectionPoints);

    return processPaths.stream().flatMap(processPath ->
            ((SimpleProcess.Context) processedContexts.getProcessContextByProcessName(processorName(processPath))).getUnprocessedBacklog()
                .stream().flatMap(unprocessedBacklogState -> generateBacklogProjected(unprocessedBacklogState, processPath)))
        .collect(Collectors.toList());
  }

  private static ParallelProcess.Context.Assistant buildParallelAssistant(final Set<ProcessPath> processPaths) {
    return new DelegateAssistant(new ProcessPathSplitter(processPaths), MERGER);
  }

  private static Map<String, ProcessContext> buildSimpleProcessContexts(
      final Map<ProcessPath, Map<Instant, Long>> currentBacklog,
      final Map<ProcessPath, Map<Instant, Integer>> throughput
  ) {
    final var orderedBacklogByProcessPath = currentBacklog.entrySet()
        .stream()
        .collect(toMap(
            Map.Entry::getKey,
            entry -> new OrderedBacklogByDate(
                entry.getValue()
                    .entrySet()
                    .stream()
                    .collect(toMap(
                        Map.Entry::getKey,
                        inner -> new Quantity(inner.getValue())
                    ))
            )
        ));

    return throughput.keySet().stream()
        .collect(
            toMap(
                Enum::toString,
                process -> new SimpleProcess.Context(
                    new ThroughputPerHour(throughput.getOrDefault(process, emptyMap())),
                    HELPER,
                    orderedBacklogByProcessPath.getOrDefault(process, OrderedBacklogByDate.emptyBacklog())
                )
            )
        );
  }

  private static Stream<ProcessPathBacklog> generateBacklogProjected(
      final UnprocessedBacklogState unprocessedBacklogState,
      final ProcessPath processPath
  ) {
    var backlogByDateOuts = (OrderedBacklogByDate) unprocessedBacklogState.getBacklog();

    return backlogByDateOuts.getBacklogs().entrySet().stream().map(
        backlogByDateOut -> new ProcessPathBacklog(
            unprocessedBacklogState.getEndDate(),
            processPath,
            ProcessName.PICKING,
            backlogByDateOut.getKey(),
            backlogByDateOut.getValue().total()
        )
    );
  }

  @Value
  public static class ProcessPathBacklog {
    Instant date;

    ProcessPath processPath;

    ProcessName process;

    Instant cpt;

    Long units;
  }

}
