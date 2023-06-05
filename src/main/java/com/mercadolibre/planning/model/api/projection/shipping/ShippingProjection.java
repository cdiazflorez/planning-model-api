package com.mercadolibre.planning.model.api.projection.shipping;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.HU_ASSEMBLY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.SALES_DISPATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.projection.BacklogProjection.buildContexts;
import static com.mercadolibre.planning.model.api.projection.BacklogProjection.buildGraph;
import static com.mercadolibre.planning.model.api.projection.BacklogProjection.buildOrderedBacklogByDateBasedProcessesContexts;
import static com.mercadolibre.planning.model.api.projection.BacklogProjection.toOrderedBacklogByDate;
import static com.mercadolibre.planning.model.api.util.DateUtils.generateInflectionPoints;
import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.flow.projection.tools.services.entities.context.ContextsHolder;
import com.mercadolibre.flow.projection.tools.services.entities.context.PiecewiseUpstream;
import com.mercadolibre.flow.projection.tools.services.entities.context.UnprocessedBacklogState;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDate;
import com.mercadolibre.flow.projection.tools.services.entities.process.Processor;
import com.mercadolibre.flow.projection.tools.services.entities.process.SequentialProcess;
import com.mercadolibre.flow.projection.tools.services.entities.process.SimpleProcess;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.backlogmanager.OrderedBacklogByProcessPath;
import com.mercadolibre.planning.model.api.projection.dto.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.api.util.DateUtils;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

public final class ShippingProjection {

  private static final int INFLECTION_WINDOW_SIZE_IN_MINUTES = 5;
  private static final String EXPEDITION = "Expedition";
  private static final String OUTBOUND_SHIPPING = "OutboundShipping";
  private static final Set<ProcessName> PROCESSES =
      Set.of(PICKING, BATCH_SORTER, WALL_IN, PACKING, PACKING_WALL, HU_ASSEMBLY, SALES_DISPATCH);

  private ShippingProjection() {
  }

  /**
   * filters inflection points that are not on the hour.
   * @param projectedBacklog resulting from the projection of backlogs
   * @return a projected backlog where inflection points that are not on the hour are removed
   */
  private static Map<ProcessName, Map<Instant, Map<Instant, Integer>>> filterByExactHour(
      Map<ProcessName, Map<Instant, Map<Instant, Integer>>> projectedBacklog) {
    return projectedBacklog.entrySet().stream()
        .collect(
            toMap(
                Map.Entry::getKey,
                value -> value.getValue().entrySet().stream()
                    .filter(entry -> DateUtils.isOnTheHour(entry.getKey()))
                    .collect(toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                    ))
            )
        );
  }

  /**
   * From a backlog representation the amount of backlog per date out is obtained.
   * @param process Process Name
   * @param backlog a type of {@link com.mercadolibre.flow.projection.tools.services.entities.context.Backlog}.
   * @return a map where the key is the date out and the value the amount of corresponding backlog
   */
  private static Map<Instant, Integer> getQuantityByDateOut(
      final ProcessName process,
      final com.mercadolibre.flow.projection.tools.services.entities.context.Backlog backlog
  ) {
    OrderedBacklogByDate byDate = PICKING.equals(process)
        ? (OrderedBacklogByDate) toOrderedBacklogByDate(((OrderedBacklogByProcessPath) backlog).getBacklogs())
        : (OrderedBacklogByDate) backlog;

    return byDate.getBacklogs().entrySet().stream()
        .collect(
            toMap(
                Map.Entry::getKey,
                bkg -> ((int) bkg.getValue().total())
            )
        );
  }

  /**
   * Responsible for creating the shipping graph, obtaining the outbound graph and adding the corresponding shipping part.
   * @return a {@link Processor} with outbound and shipping processes.
   */
  private static Processor buildShippingGraph() {
    return SequentialProcess.builder()
        .name(OUTBOUND_SHIPPING)
        .process(buildGraph())
        .process(
            SequentialProcess.builder()
                .name(EXPEDITION)
                .process(new SimpleProcess(HU_ASSEMBLY.getName()))
                .process(new SimpleProcess(SALES_DISPATCH.getName()))
                .build()
        )
        .build();
  }

  /**
   * Responsible for creating the shipping context, obtaining the outbound context and adding the corresponding shipping part.
   * @return a {@link ContextsHolder} with outbound and shipping contexts.
   */
  private static ContextsHolder buildShippingContexts(
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> backlog,
      final Map<ProcessName, Map<Instant, Integer>> throughput
  ) {
    final var contexts = buildOrderedBacklogByDateBasedProcessesContexts(backlog, throughput);
    return buildContexts(backlog, throughput)
        .oneProcessContext(HU_ASSEMBLY.getName(), contexts.get(HU_ASSEMBLY))
        .oneProcessContext(SALES_DISPATCH.getName(), contexts.get(SALES_DISPATCH))
        .build();

  }

  /**
   * Rearranges the map so that it is grouped first by hour of operation, then by process name, then by date out to quantity.
   * @param projectionMap resulting from projection
   * @return a new Map
   */
  private static Map<Instant, Map<ProcessName, Map<Instant, Integer>>> groupProjectionMapByOperationHour(
      final Map<ProcessName, Map<Instant, Map<Instant, Integer>>> projectionMap
  ) {
    return projectionMap.entrySet()
        .stream()
        .flatMap(entry -> entry.getValue()
            .entrySet()
            .stream()
            .map(backlogByOpHour -> new Tuple(
                    entry.getKey(),
                    backlogByOpHour.getKey(),
                    backlogByOpHour.getValue()
                )
            )
        )
        .collect(
            groupingBy(
                Tuple::getOperationHour,
                toMap(Tuple::getProcessName, Tuple::getBacklogQuantityBySlas)
            )
        );
  }

  private static Map<ProcessName, Map<Instant, Map<Instant, Integer>>> mapProjections(ContextsHolder processedContexts) {

    return PROCESSES.stream()
        .collect(
            toMap(
                Function.identity(),
                process -> Stream.of(process)
                    .map(ProcessName::getName)
                    .map(processedContexts::getProcessContextByProcessName)
                    .map(SimpleProcess.Context.class::cast)
                    .map(SimpleProcess.Context::getUnprocessedBacklog)
                    .flatMap(List::stream)
                    .collect(toMap(
                        UnprocessedBacklogState::getEndDate,
                        unprocessedBacklogState -> getQuantityByDateOut(process, unprocessedBacklogState.getBacklog())
                    ))
            )
        );
  }

  private static PiecewiseUpstream toPiecewiseUpstream(Map<Instant, Map<ProcessPath, Map<Instant, Long>>> forecastBacklog) {
    return new PiecewiseUpstream(
        forecastBacklog.entrySet().stream()
            .collect(toMap(
                Map.Entry::getKey,
                entry -> OrderedBacklogByProcessPath.from(entry.getValue())
            ))
    );
  }

  /**
   * From the inputs, it creates the graph, looks for the inflection points,
   * creates the context map, executes the projection,
   * then filters the inflection points that are of no interest and builds the resulting response.
   * @param executionDateFrom moment where the projection begins
   * @param executionDateTo moment where the projection ends
   * @param currentBacklog current backlog per process
   * @param forecastBacklog sales forecast from the beginning to the end of the projection
   * @param throughput throughput from the beginning to the end of the projection
   * @return a {@link  BacklogProjectionResponse}.
   */
  public static Map<Instant, Map<ProcessName, Map<Instant, Integer>>> calculateShippingProjection(
      final Instant executionDateFrom,
      final Instant executionDateTo,
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentBacklog,
      final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> forecastBacklog,
      final Map<ProcessName, Map<Instant, Integer>> throughput
  ) {
    final List<Instant> inflectionPoints = generateInflectionPoints(executionDateFrom, executionDateTo, INFLECTION_WINDOW_SIZE_IN_MINUTES);

    final var backlog = getCurrentBacklogUpdated(currentBacklog);

    final Processor graph = buildShippingGraph();

    final ContextsHolder contexts = buildShippingContexts(backlog, throughput);

    final PiecewiseUpstream piecewiseUpstream = toPiecewiseUpstream(forecastBacklog);

    final ContextsHolder processedContexts = graph.accept(contexts, piecewiseUpstream, inflectionPoints);

    final Map<ProcessName, Map<Instant, Map<Instant, Integer>>> projResults = mapProjections(
        processedContexts
    );

    final Map<ProcessName, Map<Instant, Map<Instant, Integer>>> inflectionPointsAtExactHour = filterByExactHour(projResults);

    return groupProjectionMapByOperationHour(inflectionPointsAtExactHour);
  }

  private static Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> getCurrentBacklogUpdated(
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentBacklog
  ) {
    if (currentBacklog.get(WAVING) == null) {
      return currentBacklog;
    }
    final BiFunction<Map<ProcessPath, Map<Instant, Long>>, Map<ProcessPath, Map<Instant, Long>>, Map<ProcessPath, Map<Instant, Long>>>
        merge = (left, right) ->
            Stream.concat(
                    left.entrySet().stream(),
                    right.entrySet().stream()
                )
                .collect(
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

      return PROCESSES.stream()
          .collect(
              toMap(
                  Function.identity(),
                  process -> process == PICKING
                      ? merge.apply(currentBacklog.get(WAVING), currentBacklog.get(PICKING))
                      : currentBacklog.get(process)
              )
          );
  }

  @AllArgsConstructor
  @Getter
  static class Projection {
    private ProcessName processName;
    private Instant date;
    private Instant dateOut;
    private int quantity;
  }

  @AllArgsConstructor
  @Getter
  static class Tuple {
    ProcessName processName;
    Instant operationHour;
    Map<Instant, Integer> backlogQuantityBySlas;
  }
}
