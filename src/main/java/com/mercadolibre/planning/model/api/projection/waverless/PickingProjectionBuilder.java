package com.mercadolibre.planning.model.api.projection.waverless;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.mercadolibre.flow.projection.tools.services.entities.context.ContextsHolder;
import com.mercadolibre.flow.projection.tools.services.entities.context.DelegateAssistant;
import com.mercadolibre.flow.projection.tools.services.entities.context.ProcessContext;
import com.mercadolibre.flow.projection.tools.services.entities.context.ThroughputPerHour;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDate;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDateConsumer;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.helpers.BacklogByDateHelper;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.helpers.OrderedBacklogByDateMerger;
import com.mercadolibre.flow.projection.tools.services.entities.process.ParallelProcess;
import com.mercadolibre.flow.projection.tools.services.entities.process.Processor;
import com.mercadolibre.flow.projection.tools.services.entities.process.SimpleProcess;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        .map(pp -> new SimpleProcess(pp.toString()))
        .collect(Collectors.toList());

    return new ParallelProcess(PICKING_PROCESS, processors);
  }

  public static ContextsHolder buildContextHolder(
      final Map<ProcessPath, Map<Instant, Integer>> currentBacklog,
      final Map<ProcessPath, Map<Instant, Integer>> throughput
  ) {
    final var processPaths = throughput.keySet();

    final Map<String, ProcessContext> contexts = new HashMap<>();
    contexts.put(PICKING_PROCESS, new ParallelProcess.Context(buildParallelAssistant(processPaths), emptyList()));
    contexts.putAll(buildSimpleProcessContexts(currentBacklog, throughput));

    return new ContextsHolder(contexts);
  }

  private static ParallelProcess.Context.Assistant buildParallelAssistant(final Set<ProcessPath> processPaths) {
    return new DelegateAssistant(new ProcessPathSplitter(processPaths), MERGER);
  }

  private static Map<String, ProcessContext> buildSimpleProcessContexts(
      final Map<ProcessPath, Map<Instant, Integer>> currentBacklog,
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
                        inner -> new OrderedBacklogByDate.Quantity(inner.getValue())
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
}
