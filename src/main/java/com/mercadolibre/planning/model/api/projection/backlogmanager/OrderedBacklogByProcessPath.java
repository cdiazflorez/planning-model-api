package com.mercadolibre.planning.model.api.projection.backlogmanager;

import static java.util.stream.Collectors.toMap;

import com.mercadolibre.flow.projection.tools.services.entities.context.Backlog;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDate;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.util.Map;
import java.util.function.LongUnaryOperator;
import java.util.stream.Collectors;
import lombok.Value;

@Value
public class OrderedBacklogByProcessPath implements Backlog {

  Map<ProcessPath, Backlog> backlogs;

  public static OrderedBacklogByProcessPath from(final Map<ProcessPath, Map<Instant, Long>> currentBacklog) {
    final Map<ProcessPath, Backlog> orderedBacklogByProcessPath = currentBacklog.entrySet()
        .stream()
        .collect(
            toMap(Map.Entry::getKey, entry -> new OrderedBacklogByDate(asQuantityByDate(entry.getValue())))
        );

    return new OrderedBacklogByProcessPath(orderedBacklogByProcessPath);
  }

  private static Map<Instant, OrderedBacklogByDate.Quantity> asQuantityByDate(final Map<Instant, Long> quantities) {
    return quantities.entrySet().stream()
        .collect(
            toMap(Map.Entry::getKey, entry -> new OrderedBacklogByDate.Quantity(entry.getValue()))
        );
  }

  @Override
  public long total() {
    return backlogs.values()
        .stream()
        .mapToLong(Backlog::total)
        .sum();
  }

  @Override
  public Backlog map(final LongUnaryOperator f) {
    return new OrderedBacklogByProcessPath(
        backlogs.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().map(f)
                )
            )
    );
  }

}
