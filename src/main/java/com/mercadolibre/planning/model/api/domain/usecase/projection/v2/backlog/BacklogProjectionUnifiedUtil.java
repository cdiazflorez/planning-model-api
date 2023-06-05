package com.mercadolibre.planning.model.api.domain.usecase.projection.v2.backlog;

import static java.util.stream.Collectors.toMap;

import com.mercadolibre.flow.projection.tools.services.entities.context.BacklogHelper;
import com.mercadolibre.flow.projection.tools.services.entities.context.ContextsHolder;
import com.mercadolibre.flow.projection.tools.services.entities.context.Merger;
import com.mercadolibre.flow.projection.tools.services.entities.context.PiecewiseUpstream;
import com.mercadolibre.flow.projection.tools.services.entities.context.ThroughputPerHour;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDateConsumer;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.helpers.BacklogByDateHelper;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.helpers.OrderedBacklogByDateMerger;
import com.mercadolibre.flow.projection.tools.services.entities.process.SimpleProcess;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.backlogmanager.DistributionBasedConsumer;
import com.mercadolibre.planning.model.api.projection.backlogmanager.OrderedBacklogByProcessPath;
import com.mercadolibre.planning.model.api.projection.backlogmanager.ProcessPathMerger;
import com.mercadolibre.planning.model.api.projection.dto.request.total.BacklogRequest;
import com.mercadolibre.planning.model.api.projection.dto.request.total.Throughput;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Value;
import org.springframework.stereotype.Component;

@Component
public final class BacklogProjectionUnifiedUtil {

  private static final Merger BACKLOG_BY_DATE_MERGER = new OrderedBacklogByDateMerger();

  private static final OrderedBacklogByDateConsumer BACKLOG_BY_DATE_CONSUMER = new OrderedBacklogByDateConsumer();

  private static final BacklogHelper BACKLOG_BY_PROCESS_PATH_HELPER = new BacklogByDateHelper(
      new DistributionBasedConsumer(BACKLOG_BY_DATE_CONSUMER),
      new ProcessPathMerger(BACKLOG_BY_DATE_MERGER)
  );

  private BacklogProjectionUnifiedUtil() {
  }

  static PiecewiseUpstream toPiecewiseUpstream(final BacklogRequest plannedUnit) {
    final var plannedUnitMap = plannedUnit.getProcessPath().stream()
        .flatMap(processPathRequest -> processPathRequest.getQuantity().stream()
            .map(quantity -> new BacklogForecasted(
                quantity.getDateIn(),
                quantity.getDateOut(),
                processPathRequest.getName(),
                quantity.getQuantity()
            )))
        .collect(
            Collectors.groupingBy(
                BacklogForecasted::getDateIn,
                Collectors.collectingAndThen(
                    Collectors.groupingBy(
                        BacklogForecasted::getProcessPath,
                        toMap(BacklogForecasted::getDateOut, BacklogForecasted::getQuantity, Long::sum)
                    ),
                    OrderedBacklogByProcessPath::from
                )
            )
        );

    return new PiecewiseUpstream(plannedUnitMap);
  }

  static ContextsHolder buildContexts(final BacklogRequest backlogRequest, final List<Throughput> throughput, final String nameContext) {
    final var simpleContext = new SimpleProcess.Context(
        buildThroughputPerHour(throughput),
        BACKLOG_BY_PROCESS_PATH_HELPER,
        buildOrderedBacklog(backlogRequest)
    );

    return ContextsHolder.builder()
        .oneProcessContext(nameContext, simpleContext)
        .build();
  }

  private static OrderedBacklogByProcessPath buildOrderedBacklog(final BacklogRequest backlog) {
    final var backlogByProcessPathAndDateOut = backlog.getProcessPath().stream()
        .flatMap(processPathRequest -> processPathRequest.getQuantity().stream()
            .map(quantity -> new CurrentBacklog(
                quantity.getDateOut(),
                processPathRequest.getName(),
                quantity.getQuantity()
            )))
        .collect(Collectors.groupingBy(CurrentBacklog::getProcessPath,
            Collectors.groupingBy(CurrentBacklog::getDateOut,
                Collectors.summingLong(CurrentBacklog::getQuantity))));

    return OrderedBacklogByProcessPath.from(backlogByProcessPathAndDateOut);
  }

  private static ThroughputPerHour buildThroughputPerHour(final List<Throughput> throughput) {
    final var tphByDate = throughput.stream()
        .collect(toMap(Throughput::getDate, Throughput::getQuantity));

    return new ThroughputPerHour(tphByDate);
  }

  @Value
  private static class BacklogForecasted {
    Instant dateIn;
    Instant dateOut;
    ProcessPath processPath;
    long quantity;
  }

  @Value
  private static class CurrentBacklog {
    Instant dateOut;
    ProcessPath processPath;
    long quantity;
  }
}
