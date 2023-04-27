package com.mercadolibre.planning.model.api.projection.waverless.idleness;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.util.Map;

final class InitialWaveStrategy {

  private InitialWaveStrategy() {
  }

  /**
   * Calculates a backlog distribution that matches picking throughput ratios.
   *
   * <p>Takes the buffer size of picking at the hour of the wave and divides it between the process paths
   * using the picking throughput ratio. As {@link ProcessPath#GLOBAL} does not represent a real value it is filtered.
   *
   * @param waveExecutionDate              wave execution date.
   * @param pickingThroughputByProcessPath throughput by Process Path to calculate the tph ratio.
   * @param bufferSize                     max units to wave.
   * @return a division of the available buffer by Process Path that matches de tph ratio.
   */
  static WaveDistribution matchThroughputRatio(
      final Instant waveExecutionDate,
      final long bufferSize,
      final Map<ProcessPath, Map<Instant, Integer>> pickingThroughputByProcessPath,
      final Map<ProcessPath, Integer> lowerBounds
  ) {
    final var waveExecutionHour = waveExecutionDate.truncatedTo(HOURS);
    final var waveExecutionHourThroughput = getWaveExecutionHourThroughput(waveExecutionHour, pickingThroughputByProcessPath);

    final float totalThroughput = waveExecutionHourThroughput.values()
        .stream()
        .map(Integer::floatValue)
        .reduce(0F, Float::sum);

    final var distribution = waveExecutionHourThroughput.entrySet()
        .stream()
        .collect(
            toMap(
                Map.Entry::getKey,
                entry -> unitsToWave(bufferSize, lowerBounds.get(entry.getKey()), entry.getValue(), totalThroughput)
            )
        );

    return new WaveDistribution(distribution, lowerBounds);
  }

  private static Map<ProcessPath, Integer> getWaveExecutionHourThroughput(
      final Instant waveExecutionHour,
      final Map<ProcessPath, Map<Instant, Integer>> throughputByProcessPath
  ) {
    return throughputByProcessPath.entrySet()
        .stream()
        .collect(
            toMap(
                Map.Entry::getKey,
                entry -> entry.getValue()
                    .get(waveExecutionHour)
            )
        );
  }

  private static int unitsToWave(
      final long bufferSize,
      final int lowerBound,
      final int processPathThroughput,
      final float totalThroughput
  ) {
    final var units = totalThroughput == 0
        ? 0
        : Math.round(bufferSize * (processPathThroughput / totalThroughput));

    return Math.max(units, lowerBound);
  }

}
