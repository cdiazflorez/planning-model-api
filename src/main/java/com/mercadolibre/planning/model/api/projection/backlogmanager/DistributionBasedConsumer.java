package com.mercadolibre.planning.model.api.projection.backlogmanager;

import com.mercadolibre.flow.projection.tools.services.entities.context.Backlog;
import com.mercadolibre.flow.projection.tools.services.entities.context.BacklogConsume;
import com.mercadolibre.flow.projection.tools.services.entities.context.Consumer;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDateConsumer;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.util.MathUtil;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DistributionBasedConsumer implements Consumer {

  private final OrderedBacklogByDateConsumer delegate;

  private static int calculateProcessPathProcessingPower(final int processingPower, final long processPathQuantity, final long total) {
    final var ratio = MathUtil.safeDiv((double) processPathQuantity, (double) total);
    return (int) (processingPower * ratio);
  }

  /**
   * Consumes and {@link OrderedBacklogByProcessPath} by dividing the available processing power proportionally to the total backlog
   * of each Process Paths. The actual consume operation it is realized by its delegate.
   *
   * @param startingDate     date from which the backlog begins to be consumed
   * @param endingDate       date to which the backlog is consumed
   * @param backlogToConsume backlog representation to be consumed
   * @param processingPower  processing power of units that can be consumed
   * @return consumed backlog by process path
   */
  @Override
  public BacklogConsume consume(
      final Instant startingDate,
      final Instant endingDate,
      final Backlog backlogToConsume,
      final int processingPower
  ) {
    final var backlog = (OrderedBacklogByProcessPath) backlogToConsume;
    final Map<ProcessPath, BacklogConsume> consumed = backlog.getBacklogs()
        .entrySet()
        .stream()
        .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> delegate.consume(
                    startingDate,
                    endingDate,
                    entry.getValue(),
                    calculateProcessPathProcessingPower(processingPower, entry.getValue().total(), backlogToConsume.total())
                )
            )
        );

    final Map<ProcessPath, Backlog> processed = consumed.entrySet()
        .stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().getConsumed()
        ));

    final Map<ProcessPath, Backlog> leftOver = consumed.entrySet()
        .stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().getLeftOver()
        ));

    return new BacklogConsume(new OrderedBacklogByProcessPath(processed), new OrderedBacklogByProcessPath(leftOver));
  }
}
