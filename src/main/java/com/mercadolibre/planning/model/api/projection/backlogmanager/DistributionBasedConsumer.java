package com.mercadolibre.planning.model.api.projection.backlogmanager;

import static java.util.stream.Collectors.toMap;

import com.mercadolibre.flow.projection.tools.services.entities.context.Backlog;
import com.mercadolibre.flow.projection.tools.services.entities.context.BacklogConsume;
import com.mercadolibre.flow.projection.tools.services.entities.context.Consumer;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDateConsumer;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.util.MathUtil;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DistributionBasedConsumer implements Consumer {

  private final OrderedBacklogByDateConsumer delegate;

  private final double reservedProcessingPowerRatio;

  public DistributionBasedConsumer(final OrderedBacklogByDateConsumer delegate) {
    this.delegate = delegate;
    this.reservedProcessingPowerRatio = 0D;
  }

  private static int calculateProcessPathProcessingPower(
      final int processingPower,
      final long processPathQuantity,
      final long total,
      final Long reservedProcessingPower
  ) {
    final var processPathBacklog = processPathQuantity - reservedProcessingPower;
    final var ratio = MathUtil.safeDiv((double) processPathBacklog, (double) total);
    return (int) (processingPower * ratio) + reservedProcessingPower.intValue();
  }

  /**
   * Consumes and {@link OrderedBacklogByProcessPath} by assigning a percentage of the processing power statically and
   * dividing the remaining available processing power proportionally to the total backlog of each Process Paths.
   * The actual consume operation it is realized by its delegate.
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

    final Map<ProcessPath, Long> totalBacklogByProcessPath = backlog.getBacklogs()
        .entrySet()
        .stream()
        .collect(toMap(Map.Entry::getKey, entry -> entry.getValue().total()));

    final var minBacklogToConsume = (int) (processingPower * reservedProcessingPowerRatio);

    final var minBacklogToConsumeByProcessPath = totalBacklogByProcessPath.entrySet().stream()
        .collect(toMap(Map.Entry::getKey, entry -> Math.min(entry.getValue(), minBacklogToConsume)));
    final var reservedProcessingPower = minBacklogToConsumeByProcessPath.values().stream().reduce(0L, Long::sum).intValue();

    final var totalBacklog = backlogToConsume.total() - reservedProcessingPower;
    final var assignableProcessingPower = processingPower - reservedProcessingPower;

    final var processingPowerByProcessPath = totalBacklogByProcessPath.entrySet()
        .stream()
        .collect(
            toMap(
                Map.Entry::getKey,
                entry -> calculateProcessPathProcessingPower(
                    assignableProcessingPower,
                    entry.getValue(),
                    totalBacklog,
                    minBacklogToConsumeByProcessPath.get(entry.getKey()
                    )
                )
            )
        );

    final Map<ProcessPath, BacklogConsume> consumed = backlog.getBacklogs()
        .entrySet()
        .stream()
        .collect(toMap(
                Map.Entry::getKey,
                entry -> delegate.consume(
                    startingDate,
                    endingDate,
                    entry.getValue(),
                    processingPowerByProcessPath.get(entry.getKey())
                )
            )
        );

    final Map<ProcessPath, Backlog> processed = consumed.entrySet()
        .stream()
        .collect(toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().getConsumed()
        ));

    final Map<ProcessPath, Backlog> leftOver = consumed.entrySet()
        .stream()
        .collect(toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().getLeftOver()
        ));

    return new BacklogConsume(new OrderedBacklogByProcessPath(processed), new OrderedBacklogByProcessPath(leftOver));
  }
}
