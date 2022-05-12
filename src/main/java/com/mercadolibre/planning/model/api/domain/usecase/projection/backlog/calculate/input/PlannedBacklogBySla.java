package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input;

import static com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.BacklogBySla.emptyBacklog;

import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionService.IncomingBacklog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.QuantityAtDate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Value;

/**
 * Forecasted backlog by hour and sla representation.
 */
@Value
public class PlannedBacklogBySla implements IncomingBacklog<BacklogBySla> {
  private static final double MINUTES_IN_HOUR = 60.0;

  Map<Instant, BacklogBySla> plannedUnits;

  @Override
  public BacklogBySla get(final Instant operatingHour) {
    final var truncated = operatingHour.truncatedTo(ChronoUnit.HOURS);

    final var minutes = operatingHour.atZone(ZoneOffset.UTC).getMinute();
    final var remainingHourFraction = (MINUTES_IN_HOUR - minutes) / MINUTES_IN_HOUR;

    final var backlog = plannedUnits.get(truncated);

    if (backlog == null) {
      return emptyBacklog();
    }

    if (remainingHourFraction == MINUTES_IN_HOUR) {
      return backlog;
    } else {
      final var distributions = backlog.getDistributions();
      return new BacklogBySla(
          distributions.stream()
              .map(quantity -> new QuantityAtDate(quantity.getDate(), (int) (quantity.getQuantity() * remainingHourFraction)))
              .collect(Collectors.toList())
      );
    }
  }
}
