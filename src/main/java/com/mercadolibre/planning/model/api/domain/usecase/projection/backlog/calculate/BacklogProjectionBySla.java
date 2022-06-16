package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate;

import static com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionService.project;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionService.IncomingBacklog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.helper.BacklogBySlaHelper;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.BacklogBySla;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.PlannedBacklogBySla;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.ThroughputByHour;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.UpstreamBacklog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.ProjectionResult;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * BacklogProjectionBySla loops over the processes invoking the backlog projection and storing the resulting state.
 */
@Component
public class BacklogProjectionBySla {

  public Map<ProcessName, List<ProjectionResult<BacklogBySla>>> execute(
      final Instant dateFrom,
      final Instant dateTo,
      final Workflow workflow,
      final List<ProcessName> processNames,
      final Map<ProcessName, ThroughputByHour> throughputByProcess,
      final PlannedBacklogBySla plannedBacklog,
      final Map<ProcessName, BacklogBySla> backlog) {

    final var helper = new BacklogBySlaHelper();

    IncomingBacklog<BacklogBySla> upstreamBacklog = plannedBacklog;
    final Map<ProcessName, List<ProjectionResult<BacklogBySla>>> result = new EnumMap<>(ProcessName.class);
    for (ProcessName process : processNames) {
      final List<ProjectionResult<BacklogBySla>> processResult = project(
          dateFrom,
          dateTo,
          upstreamBacklog,
          backlog.get(process),
          throughputByProcess.get(process),
          helper
      );

      upstreamBacklog = toUpstreamBacklog(processResult);
      result.put(process, processResult);
    }

    return result;
  }

  private UpstreamBacklog toUpstreamBacklog(final List<ProjectionResult<BacklogBySla>> upstreamResults) {
    final Map<Instant, BacklogBySla> backlogByOperatingHour = upstreamResults.stream()
        .collect(Collectors.toMap(
            ProjectionResult::getOperatingHour,
            result -> result.getResultingState().getProcessed()
        ));
    return new UpstreamBacklog(backlogByOperatingHour);
  }
}
