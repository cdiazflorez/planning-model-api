package com.mercadolibre.planning.model.api.projection.availablecapacity;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * A service for calculating the sum of values within a specified time range based on a map of time-value pairs.
 */
public final class ThroughputCalculator {

  private static final double MINUTES_IN_A_HOUR = 60.0;

  private ThroughputCalculator() {
  }

  /**
   * Calculates the sum of values within the specified time range based on the provided time-value pairs.
   *
   * @param tphByHour A map of time-value pairs, where each entry represents a time
   *                  and an associated integer value.
   * @param from      The starting time of the desired range (inclusive).
   * @param to        The ending time of the desired range (inclusive).
   * @return The sum of values within the specified time range.
   */
  public static int totalWithinRange(final Map<Instant, Integer> tphByHour, final Instant from, final Instant to) {
    return tphByHour.entrySet().stream()
        .mapToInt(entry -> calculateValueInRange(entry.getValue(), calculateFractionOfHour(entry.getKey(), from, to)))
        .sum();
  }

  private static Duration calculateFractionOfHour(final Instant hour, final Instant from, final Instant to) {
    final Instant start = hour.isAfter(from) ? hour : from;
    final Instant end = hour.plus(Duration.ofHours(1)).isBefore(to) ? hour.plus(Duration.ofHours(1)) : to;
    return Duration.between(start, end);
  }


  private static int calculateValueInRange(final int value, final Duration fractionOfHour) {
    if (fractionOfHour.toMinutes() <= 0) {
      return 0;
    }
    final double proportion = fractionOfHour.toMinutes() / MINUTES_IN_A_HOUR;
    return (int) (value * proportion);
  }
}
