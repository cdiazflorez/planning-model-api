package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input;

import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionService.Throughput;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import lombok.Value;

/**
 * Available throughput for a specific process and hour.
 */
@Value
public class ThroughputByHour implements Throughput {
  private static final double MINUTES_IN_HOUR = 60.0;

  Map<Instant, Integer> tphByHour;

  @Override
  public int getAvailableQuantityFor(final Instant operatingHour) {
    final var truncated = operatingHour.truncatedTo(ChronoUnit.HOURS);

    final var minutes = operatingHour.atZone(ZoneOffset.UTC).getMinute();
    final var remainingHourFraction = (MINUTES_IN_HOUR - minutes) / MINUTES_IN_HOUR;

    return (int) (tphByHour.getOrDefault(truncated, 0) * remainingHourFraction);
  }
}
