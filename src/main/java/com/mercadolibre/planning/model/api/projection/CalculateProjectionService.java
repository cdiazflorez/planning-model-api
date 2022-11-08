package com.mercadolibre.planning.model.api.projection;

import static com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.utils.OrderedBacklogByDateUtils.calculateProjectedEndDate;
import static com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.utils.OrderedBacklogByDateUtils.calculateRemainingQuantity;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.flow.projection.tools.services.entities.context.ContextsHolder;
import com.mercadolibre.flow.projection.tools.services.entities.context.DelegateAssistant;
import com.mercadolibre.flow.projection.tools.services.entities.context.PiecewiseUpstream;
import com.mercadolibre.flow.projection.tools.services.entities.context.ThroughputPerHour;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDate;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDateConsumer;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.helpers.BacklogByDateHelper;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.helpers.OrderedBacklogByDateMerger;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.helpers.OrderedBacklogByDateRatioSplitter;
import com.mercadolibre.flow.projection.tools.services.entities.process.ParallelProcess;
import com.mercadolibre.flow.projection.tools.services.entities.process.SequentialProcess;
import com.mercadolibre.flow.projection.tools.services.entities.process.SimpleProcess;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.exception.UnsupportedWorkflowException;
import com.mercadolibre.planning.model.api.projection.dto.ProjectionRequest;
import com.mercadolibre.planning.model.api.projection.dto.ProjectionResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CalculateProjectionService {

  private static final long BACKLOG_ZERO = 0;

  private static final String CONSOLIDATION_PROCESS_GROUP = "consolidation_group";

  private static final OrderedBacklogByDate EMPTY_BACKLOG = new OrderedBacklogByDate(emptyMap());

  private static final String PACKING_PROCESS_GROUP = "packing_group";

  private static final List<ProcessName> OUTBOUND_PROCESSES = List.of(WAVING, PICKING, PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL);

  private static final OrderedBacklogByDateRatioSplitter.Distribution<String> DEFAULT_PACKING_DISTRIBUTION =
      new OrderedBacklogByDateRatioSplitter.Distribution<>(Map.of(PACKING.getName(), 0.5, CONSOLIDATION_PROCESS_GROUP, 0.5));

  /**
   * Calculate projection about params.
   *
   * @param workflow          workflow
   * @param projectionRequest request
   * @return projections
   */
  public List<ProjectionResult> execute(final Workflow workflow, final ProjectionRequest projectionRequest) {

    final var inflectionPoints = ProjectionUtils.generateInflectionPoints(projectionRequest.getDateFrom(), projectionRequest.getDateTo());

    final var ratios = ProjectionUtils.ratiosAsDistributions(projectionRequest.getRatioByHour(), inflectionPoints);

    final PiecewiseUpstream forecastedBacklog = ProjectionUtils.mapForecastToUpstreamBacklog(projectionRequest.getForecastSales());

    final SequentialProcess globalSequentialProcess = buildProcessGraph(workflow);
    final ContextsHolder context = buildContextsHolder(
        projectionRequest.getBacklogBySlaAndProcess(),
        projectionRequest.getThroughputByProcess(),
        ratios);

    final Set<Instant> dateOuts = ProjectionUtils.obtainDateOutsFrom(projectionRequest.getBacklogBySlaAndProcess());

    final ContextsHolder projection = globalSequentialProcess.accept(context, forecastedBacklog, inflectionPoints);

    final Map<Instant, Instant> projectedEndDateByDateOut = calculateProjectedEndDate(
        projection, Workflow.FBM_WMS_OUTBOUND.getName(), new ArrayList<>(dateOuts)
    );

    final List<String> processes = OUTBOUND_PROCESSES.stream()
        .map(ProcessName::getName)
        .collect(Collectors.toList());

    final Map<Instant, Long> remainingQuantityByDateOut = calculateRemainingQuantity(projection, processes, new ArrayList<>(dateOuts));

    return mappingResponseService(dateOuts, projectedEndDateByDateOut, remainingQuantityByDateOut, projectionRequest.getDateFrom());
  }

  private SequentialProcess buildProcessGraph(final Workflow workflow) {
    if (workflow == Workflow.FBM_WMS_OUTBOUND) {
      return SequentialProcess.builder()
          .name(workflow.getName())
          .process(new SimpleProcess(WAVING.getName()))
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
    } else {
      throw new UnsupportedWorkflowException();
    }

  }

  private ContextsHolder buildContextsHolder(
      final Map<ProcessName, Map<Instant, Integer>> currentBacklog,
      final Map<ProcessName, Map<Instant, Integer>> throughputByProcess,
      final Map<Instant, OrderedBacklogByDateRatioSplitter.Distribution<String>> packingDistributionRatios
  ) {
    final var processSimpleProcessContexts = generateSimpleProcessContexts(currentBacklog, throughputByProcess);
    final var assistant = getPackingAssistant(packingDistributionRatios);

    return ContextsHolder.builder()
        .oneProcessContext(WAVING.getName(), processSimpleProcessContexts.get(WAVING))
        .oneProcessContext(PICKING.getName(), processSimpleProcessContexts.get(PICKING))
        .oneProcessContext(BATCH_SORTER.getName(), processSimpleProcessContexts.get(BATCH_SORTER))
        .oneProcessContext(WALL_IN.getName(), processSimpleProcessContexts.get(WALL_IN))
        .oneProcessContext(PACKING.getName(), processSimpleProcessContexts.get(PACKING))
        .oneProcessContext(PACKING_WALL.getName(), processSimpleProcessContexts.get(PACKING_WALL))
        .oneProcessContext(PACKING_PROCESS_GROUP, new ParallelProcess.Context(assistant, emptyList()))
        .build();
  }

  private DelegateAssistant getPackingAssistant(
      final Map<Instant, OrderedBacklogByDateRatioSplitter.Distribution<String>> packingDistributionRatios) {
    return new DelegateAssistant(
        new OrderedBacklogByDateRatioSplitter(packingDistributionRatios, DEFAULT_PACKING_DISTRIBUTION),
        new OrderedBacklogByDateMerger()
    );
  }

  private Map<ProcessName, SimpleProcess.Context> generateSimpleProcessContexts(
      final Map<ProcessName, Map<Instant, Integer>> currentBacklog,
      final Map<ProcessName, Map<Instant, Integer>> throughputByProcess
  ) {
    final var helper = new BacklogByDateHelper(
        new OrderedBacklogByDateConsumer(),
        new OrderedBacklogByDateMerger()
    );

    final var backlogQuantityByProcess = currentBacklog.entrySet()
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

    return OUTBOUND_PROCESSES.stream()
        .collect(
            toMap(
                Function.identity(),
                process -> new SimpleProcess.Context(
                    new ThroughputPerHour(throughputByProcess.getOrDefault(process, emptyMap())),
                    helper,
                    backlogQuantityByProcess.getOrDefault(process, EMPTY_BACKLOG)
                )
            )
        );
  }

  private List<ProjectionResult> mappingResponseService(
      final Set<Instant> slas,
      final Map<Instant, Instant> projectedEndDateByDateOut,
      final Map<Instant, Long> remainingQuantityByDateOut,
      final Instant defaultEndDate) {

    return slas.stream().map(
        sla -> new ProjectionResult(
            sla,
            null,
            projectedEndDateByDateOut.getOrDefault(Instant.from(sla), defaultEndDate),
            Math.toIntExact(remainingQuantityByDateOut.getOrDefault(sla, BACKLOG_ZERO))
        )
    ).collect(Collectors.toList());
  }
}



