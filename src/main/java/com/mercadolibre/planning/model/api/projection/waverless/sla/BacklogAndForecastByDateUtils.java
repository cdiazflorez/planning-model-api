package com.mercadolibre.planning.model.api.projection.waverless.sla;

import com.mercadolibre.flow.projection.tools.services.entities.context.ContextsHolder;
import com.mercadolibre.flow.projection.tools.services.entities.process.SimpleProcess;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class BacklogAndForecastByDateUtils {

  private BacklogAndForecastByDateUtils() {
  }

  public static Map<Instant, Instant> calculateProjectedEndDate(
      final ContextsHolder contextsHolder,
      final String processName,
      final List<Instant> dateOuts
  ) {

    final List<BacklogAndForecastByDate> processedBacklogFromProcess = contextsHolder.getProcessContextByProcessName(processName)
        .getProcessedBacklog().stream()
        .map((processedBacklogState -> (BacklogAndForecastByDate) processedBacklogState.getBacklog()))
        .toList();

    final List<BacklogAndForecastByDate> lastUnprocessedBacklogsFromNonFinalSimpleProcesses =
        contextsHolder.getProcessContextByProcessName()
            .values().stream()
            .filter(processContext -> processContext instanceof SimpleProcess.Context)
            .map(processContext -> ((SimpleProcess.Context) processContext).getLastUnprocessedBacklogState())
            .map(orderedBacklogByDate -> (BacklogAndForecastByDate) orderedBacklogByDate.getBacklog())
            .toList();

    return calculateProjectedEndDate(
        lastUnprocessedBacklogsFromNonFinalSimpleProcesses,
        processedBacklogFromProcess,
        dateOuts
    );
  }


  /**
   * from the last simple unprocessed processes and the processes already,
   * an Instant map is obtained giving a broader solution,
   * and it is not tied to the process that is passed by parameter.
   * Note that in case of not being able to calculate a date out, it will return null for that.
   * Note that this implementation works only with {@link BacklogAndForecastByDate}
   *
   * @param lastUnprocessedBacklogsFromNonFinalSimpleProcesses is a list {@link BacklogAndForecastByDate} of raw backlogs obtained
   *                                                           from simple processes.
   * @param processedBacklogFromLastProcess                    is a list {@link BacklogAndForecastByDate} of backlogs with
   *                                                           the last processes already.
   * @param dateOuts                                           list of instants that indicate the closing date
   * @return a map with dateOut and the projected end date
   */
  public static Map<Instant, Instant> calculateProjectedEndDate(
      final List<BacklogAndForecastByDate> lastUnprocessedBacklogsFromNonFinalSimpleProcesses,
      final List<BacklogAndForecastByDate> processedBacklogFromLastProcess,
      final List<Instant> dateOuts
  ) {

    final Set<Instant> leftOverUnitsByDateOut = leftOverUnitsGroupedByDateOut(lastUnprocessedBacklogsFromNonFinalSimpleProcesses);

    final Map<Instant, Instant> projectedEndDateByDateOut = new HashMap<>();
    dateOuts.forEach(dateOut -> {
          if (leftOverUnitsByDateOut.contains(dateOut)) {
            projectedEndDateByDateOut.put(dateOut, null);
          } else {
            projectedEndDateByDateOut.put(dateOut, getProjectedInstant(dateOut, processedBacklogFromLastProcess).orElse(null));
          }
        }
    );

    return projectedEndDateByDateOut;
  }

  private static Set<Instant> leftOverUnitsGroupedByDateOut(
      final List<BacklogAndForecastByDate> lastUnprocessedBacklogsFromNonFinalSimpleProcesses
  ) {
    return lastUnprocessedBacklogsFromNonFinalSimpleProcesses.stream()
        .flatMap(orderedBacklogByDate -> orderedBacklogByDate.getBacklogs().entrySet().stream())
        .filter(a -> a.getValue().total() != 0)
        .map(Map.Entry::getKey)
        .collect(Collectors.toUnmodifiableSet());
  }

  private static Optional<Instant> getProjectedInstant(final Instant dateOut,
                                                       final List<BacklogAndForecastByDate> processedBacklogs) {
    return processedBacklogs.stream()
        .map(orderedBacklogByDate ->
            Optional.ofNullable((BacklogAndForecastByDate.QuantityWithEndDate) orderedBacklogByDate.getBacklogs().get(dateOut))
                .map(BacklogAndForecastByDate.QuantityWithEndDate::getEndDate)
        )
        .filter(Optional::isPresent)
        .map(Optional::get)
        .max(Instant::compareTo);
  }

}
