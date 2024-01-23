package com.mercadolibre.planning.model.api.projection.waverless.idleness;

import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

final class BoundsCalculator {

  private static final int MINUTES_IN_AN_HOUR = 60;

  private BoundsCalculator() {

  }

  /**
   * Calculate the bound according to the processing minutes.
   *
   * @param minutesToProcess        Number of minutes from which the Bound is required to be calculated.
   * @param waveExecutionDate       Instant from which the bound is required to be calculated for each Process Path.
   * @param throughputByProcessPath TPH by process path.
   * @return Map of bounds mapped by Process Path.
   */
  public static Map<ProcessPath, Integer> execute(
      final Map<String, Integer> minutesToProcess,
      final Instant waveExecutionDate,
      final Map<ProcessPath, Map<Instant, Integer>> throughputByProcessPath
  ) {
    return throughputByProcessPath.entrySet().stream()
        .filter(entry -> entry.getKey() != ProcessPath.GLOBAL)
        .collect(
            toMap(
                Map.Entry::getKey,
                entry -> Optional.of(entry)
                    .map(Map.Entry::getValue)
                    .map(tph -> calculateBound(
                        minutesToProcess.getOrDefault(entry.getKey().toJson(), minutesToProcess.get("default")),
                        waveExecutionDate, tph))
                    .orElse(0)
            )
        );
  }

  private static Integer calculateBound(
      final Integer minutesToProcess,
      final Instant waveExecutionDate,
      final Map<Instant, Integer> tphByInstant
  ) {
    Instant startTime = waveExecutionDate.truncatedTo(ChronoUnit.HOURS);
    Instant finalHour = waveExecutionDate.plus(minutesToProcess, ChronoUnit.MINUTES);

    int minutesExecutionDate = waveExecutionDate.atZone(ZoneOffset.UTC).getMinute();
    int remainingMinutes = minutesToProcess;
    int minutesToConsume = (MINUTES_IN_AN_HOUR - minutesExecutionDate) > minutesToProcess
        ? minutesToProcess
        : (MINUTES_IN_AN_HOUR - minutesExecutionDate);
    int tphTotal = 0;

    for (Instant index = startTime; index.isBefore(finalHour); index = index.plus(1L, ChronoUnit.HOURS)) {

      int currentHourTph = tphByInstant.getOrDefault(index, 0);
      tphTotal += currentHourTph * minutesToConsume / MINUTES_IN_AN_HOUR;

      remainingMinutes = remainingMinutes - minutesToConsume;
      minutesToConsume = Math.min(remainingMinutes, MINUTES_IN_AN_HOUR);
    }

    return tphTotal;
  }

}
