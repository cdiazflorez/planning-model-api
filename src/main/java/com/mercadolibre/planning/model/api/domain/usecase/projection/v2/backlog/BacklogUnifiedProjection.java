package com.mercadolibre.planning.model.api.domain.usecase.projection.v2.backlog;

import static com.mercadolibre.planning.model.api.domain.usecase.projection.v2.backlog.BacklogProjectionUnifiedUtil.buildContexts;
import static com.mercadolibre.planning.model.api.domain.usecase.projection.v2.backlog.BacklogProjectionUnifiedUtil.toPiecewiseUpstream;

import com.mercadolibre.flow.projection.tools.services.entities.context.ContextsHolder;
import com.mercadolibre.flow.projection.tools.services.entities.context.UnprocessedBacklogState;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDate;
import com.mercadolibre.flow.projection.tools.services.entities.process.Processor;
import com.mercadolibre.flow.projection.tools.services.entities.process.SimpleProcess;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.backlogmanager.OrderedBacklogByProcessPath;
import com.mercadolibre.planning.model.api.projection.dto.request.total.BacklogProjectionTotalRequest;
import com.mercadolibre.planning.model.api.util.DateUtils;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Value;
import org.springframework.stereotype.Component;

@Component
public class BacklogUnifiedProjection {

  private static final String SIMPLE_PROCESS_NAME = "process_unified";

  public Map<Instant, Map<Instant, Map<ProcessPath, Long>>> getProjection(
      final BacklogProjectionTotalRequest backlog,
      final int intervalSizeInMinutes) {

    final var inflectionPoints = DateUtils.generateInflectionPoints(backlog.getDateFrom(), backlog.getDateTo(), intervalSizeInMinutes);

    final var upstream = toPiecewiseUpstream(backlog.getPlannedUnit());

    final var context = buildContexts(backlog.getBacklog(), backlog.getThroughput(), SIMPLE_PROCESS_NAME);

    final Processor graph = new SimpleProcess(SIMPLE_PROCESS_NAME);

    final var processedContext = graph.accept(context, upstream, inflectionPoints);

    return extractProjectionBacklog(processedContext);
  }

  private Map<Instant, Map<Instant, Map<ProcessPath, Long>>> extractProjectionBacklog(final ContextsHolder processedContext) {
    final var backlogState = processedContext.getProcessContextByProcessName().values().stream()
        .map(SimpleProcess.Context.class::cast)
        .map(SimpleProcess.Context::getUnprocessedBacklog)
        .flatMap(List::stream);

    final var backlogNoProcessed = toBacklogByDateProcessPathAndDateOut(backlogState);

    final var backlogProcessed = toBacklogByDateAndDateOutAndProcessPath(backlogNoProcessed);

    return backlogProcessed.entrySet().stream()
        .filter(value -> DateUtils.isOnTheHour(value.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Map<Instant, Map<ProcessPath, Map<Instant, Long>>> toBacklogByDateProcessPathAndDateOut(
      final Stream<UnprocessedBacklogState> unprocessedBacklogStateStream) {

    return unprocessedBacklogStateStream.collect(Collectors.toMap(UnprocessedBacklogState::getEndDate,
            backlogState -> ((OrderedBacklogByProcessPath) backlogState.getBacklog()).getBacklogs()
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                    backlogByProcessPath -> ((OrderedBacklogByDate) backlogByProcessPath.getValue())
                        .getBacklogs().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, backlogByDateOut -> backlogByDateOut.getValue().total())))
                )
        )
    );
  }

  private Map<Instant, Map<Instant, Map<ProcessPath, Long>>> toBacklogByDateAndDateOutAndProcessPath(
      Map<Instant, Map<ProcessPath, Map<Instant, Long>>> backlogNoProcessed) {

    return backlogNoProcessed.entrySet().stream()
        .flatMap(backlogByDate -> backlogByDate.getValue().entrySet().stream()
            .flatMap(backlogByProcessPath -> backlogByProcessPath.getValue().entrySet().stream()
                .map(backlogByDateOut -> new Projection(
                    backlogByDate.getKey(),
                    backlogByProcessPath.getKey(),
                    backlogByDateOut.getKey(),
                    backlogByDateOut.getValue()))))
        .collect(
            Collectors.groupingBy(Projection::getDate,
                Collectors.groupingBy(Projection::getDateOut,
                    Collectors.groupingBy(Projection::getProcessPath, Collectors.summingLong(Projection::getQuantity)))));
  }


  @Value
  private static class Projection {
    Instant date;
    ProcessPath processPath;
    Instant dateOut;
    long quantity;
  }
}
