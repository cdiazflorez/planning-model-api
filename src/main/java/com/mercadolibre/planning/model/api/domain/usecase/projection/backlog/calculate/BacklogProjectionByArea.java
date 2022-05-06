package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate;

import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.helper.BacklogMapper;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.BacklogByArea;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.BacklogBySla;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.PlannedBacklogBySla;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.ThroughputByHour;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.ProjectionResult;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.SimpleProcessedBacklog;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Backlog projection by area calculator.
 */
@Component
@AllArgsConstructor
public class BacklogProjectionByArea {

  private BacklogProjectionBySla projector;

  public Map<ProcessName, List<ProjectionResult<BacklogByArea>>> execute(
      final Instant dateFrom,
      final Instant dateTo,
      final Workflow workflow,
      final List<ProcessName> processNames,
      final Map<ProcessName, ThroughputByHour> throughput,
      final PlannedBacklogBySla planningUnits,
      final Map<ProcessName, BacklogBySla> currentBacklogs,
      final BacklogMapper<BacklogBySla, BacklogByArea> mapper) {

    final Map<ProcessName, List<ProjectionResult<BacklogBySla>>> projections = projector.execute(
        dateFrom,
        dateTo,
        workflow,
        processNames,
        throughput,
        planningUnits,
        currentBacklogs
    );

    return projections.entrySet()
        .stream()
        .collect(toMap(
            Map.Entry::getKey,
            entry -> toBacklogByArea(entry.getKey(), entry.getValue(), mapper)
        ));
  }

  private List<ProjectionResult<BacklogByArea>> toBacklogByArea(
      final ProcessName process,
      final List<ProjectionResult<BacklogBySla>> projections,
      final BacklogMapper<BacklogBySla, BacklogByArea> backlogMapper) {

    return projections.stream()
        .map(projection -> {
              final var state = projection.getResultingState();

              return new ProjectionResult<>(
                  projection.getOperatingHour(),
                  new SimpleProcessedBacklog<>(
                      backlogMapper.map(process, state.getProcessed()),
                      backlogMapper.map(process, state.getCarryOver())
                  )
              );
            }
        ).collect(Collectors.toList());
  }

}
