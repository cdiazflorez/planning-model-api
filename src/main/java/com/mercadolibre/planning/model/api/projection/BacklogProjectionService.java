package com.mercadolibre.planning.model.api.projection;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.projection.BacklogProjection.toOrderedBacklogByDate;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.flow.projection.tools.services.entities.context.Backlog;
import com.mercadolibre.flow.projection.tools.services.entities.context.ContextsHolder;
import com.mercadolibre.flow.projection.tools.services.entities.context.UnprocessedBacklogState;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDate;
import com.mercadolibre.flow.projection.tools.services.entities.process.SimpleProcess;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.backlogmanager.OrderedBacklogByProcessPath;
import com.mercadolibre.planning.model.api.projection.builder.Projector;
import com.mercadolibre.planning.model.api.util.DateUtils;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Triple;

public final class BacklogProjectionService {

  private BacklogProjectionService() {
  }

  /**
   * This method transforms a Map of backlog projections, regrouping the data first by operation hour,
   * then by process name, and finally by the ending date mapped out to the quantity of backlog.
   * The input to this method is a Map with the process names at the top level keys, and the values are
   * another Maps which contain the backlog's ending date mapped to its respective quantity.
   * Finally, this method returns a new Map that has Instant of the operation hours as top level keys,
   * then the process names followed by another Map with the backlog's ending date mapped to its quantity.
   *
   * @param projectionMap a Map of backlog projections that follows Map<ProcessName, Map<Instant, Map<Instant, Integer>>> structure
   * @return a new Map of the structure Map<Instant, Map<ProcessName, Map<Instant, Integer>>> where the data is regrouped first
   * by operation hour, then by process name, and finally by the ending date out to the quantity of backlog.
   */
  private static Map<Instant, Map<ProcessName, Map<Instant, Integer>>> groupProjectionMapByOperationHour(
      final Map<ProcessName, Map<Instant, Map<Instant, Integer>>> projectionMap
  ) {
    return projectionMap.entrySet()
        .stream()
        .flatMap(entry -> entry.getValue()
            .entrySet()
            .stream()
            .map(backlogByOpHour -> Triple.of(
                    entry.getKey(),
                    backlogByOpHour.getKey(),
                    backlogByOpHour.getValue()
                )
            )
        )
        .collect(
            groupingBy(
                Triple::getMiddle,
                toMap(Triple::getLeft, Triple::getRight)
            )
        );
  }

  private static Map<ProcessName, Map<Instant, Map<Instant, Integer>>> transformToBacklogProjectionAndFilterNonExactHours(
      List<ProcessName> processNames, ContextsHolder processedContexts
  ) {
    return processNames.stream()
        .collect(
            toMap(
                Function.identity(),
                process -> Stream.of(process)
                    .map(ProcessName::getName)
                    .map(processedContexts::getProcessContextByProcessName)
                    .map(SimpleProcess.Context.class::cast)
                    .map(SimpleProcess.Context::getUnprocessedBacklog)
                    .flatMap(unprocessedBacklogStates -> unprocessedBacklogStates.stream()
                        .filter(unprocessedBacklogState -> DateUtils.isOnTheHour(unprocessedBacklogState.getEndDate())))
                    .collect(toMap(
                        UnprocessedBacklogState::getEndDate,
                        unprocessedBacklogState -> getQuantityByDateOut(process, unprocessedBacklogState.getBacklog())
                    ))
            )
        );
  }

  /**
   * From a backlog representation the amount of backlog per date out is obtained.
   *
   * @param process Process Name
   * @param backlog a type of {@link com.mercadolibre.flow.projection.tools.services.entities.context.Backlog}.
   * @return a map where the key is the date out and the value the amount of corresponding backlog
   */
  private static Map<Instant, Integer> getQuantityByDateOut(
      final ProcessName process,
      final Backlog backlog
  ) {
    final List<ProcessName> prePackingProcess = List.of(WAVING, PICKING);

    OrderedBacklogByDate byDate = prePackingProcess.contains(process)
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

  public static Map<Instant, Map<ProcessName, Map<Instant, Integer>>> execute(
      final Instant executionDateFrom,
      final Instant executionDateTo,
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentBacklog,
      final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> forecastBacklog,
      final Map<ProcessName, Map<Instant, Integer>> throughput,
      final List<ProcessName> processes,
      final Projector projector
  ) {
    final ContextsHolder updatedContext =
        Projection.execute(executionDateFrom, executionDateTo, currentBacklog, forecastBacklog, throughput, projector);

    final var backlogProjection = transformToBacklogProjectionAndFilterNonExactHours(processes, updatedContext);

    return groupProjectionMapByOperationHour(backlogProjection);
  }
}
