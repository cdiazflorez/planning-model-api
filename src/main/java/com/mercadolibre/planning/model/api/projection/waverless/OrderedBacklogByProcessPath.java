package com.mercadolibre.planning.model.api.projection.waverless;

import com.mercadolibre.flow.projection.tools.services.entities.context.Backlog;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.util.Map;
import java.util.function.LongUnaryOperator;
import java.util.stream.Collectors;
import lombok.Value;

@Value
public class OrderedBacklogByProcessPath implements Backlog {

  Map<ProcessPath, Backlog> backlogs;

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
