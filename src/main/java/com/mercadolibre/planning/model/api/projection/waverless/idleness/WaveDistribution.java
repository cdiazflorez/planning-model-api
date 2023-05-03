package com.mercadolibre.planning.model.api.projection.waverless.idleness;

import static com.mercadolibre.planning.model.api.util.MathUtil.safeDiv;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.TriggerName;
import com.mercadolibre.planning.model.api.projection.waverless.PendingBacklog;
import com.mercadolibre.planning.model.api.projection.waverless.Wave;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Value;

/**
 * WaveDistribution represents distribution of units to wave by Process Path.
 *
 * <p>The distribution can be interpreted as a {@link Wave} with its {@link #asWave} method
 * or as ratios by process path with the {@link  #ratios()}.
 *
 * <p>An adjustment can be applied to the wave distribution, applying an adjustment produces a new WaveDistribution that adds
 * the current units to those expressed by the adjustment. The adjusted wave distribution has a lower bound of zero units by process path
 * and an upper bound of as many units there is in the maxWaveableUnits parameter for that Process Path.
 */
@Value
public class WaveDistribution {

  Map<ProcessPath, Integer> distribution;

  Map<ProcessPath, Integer> lowerBounds;

  private static Map<ProcessPath, Map<Instant, Long>> waveableBacklog(
      final Instant waveDate,
      final List<Wave> previousWaves,
      final PendingBacklog pendingBacklog,
      final Map<ProcessPath, Integer> waveDistribution,
      final List<ProcessPath> processPaths
  ) {
    final var availableBacklog = pendingBacklog.availableBacklogAt(waveDate, processPaths, previousWaves);
    return processPaths.stream()
        .collect(
            toMap(
                Function.identity(),
                pp -> splitBacklogToWaveBySla(waveDistribution.get(pp), availableBacklog.get(pp))
            )
        );
  }

  private static Map<Instant, Long> splitBacklogToWaveBySla(final int maxUnitsToWave, final Map<Instant, Long> availableBacklog) {
    final Map<Instant, Long> result = new HashMap<>();
    availableBacklog.entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(entry -> {
              var accumulatedUnits = result.values().stream().reduce(0L, Long::sum);
              var unitsToAdd = Math.min(maxUnitsToWave - accumulatedUnits, entry.getValue());
              if (unitsToAdd > 0) {
                result.put(entry.getKey(), unitsToAdd);
              }
            }
        );

    return result;
  }

  Wave asWave(final Instant waveDate, final PendingBacklog pendingBacklog, final List<Wave> previousWaves) {
    final var processPaths = new ArrayList<>(distribution.keySet());
    final var unitsByProcessPath = waveableBacklog(waveDate, previousWaves, pendingBacklog, distribution, processPaths);

    final var confs = processPaths.stream()
        .collect(
            toMap(
                Function.identity(),
                pp -> new Wave.WaveConfiguration(0, 0, unitsByProcessPath.get(pp))
            )
        );

    return new Wave(waveDate, TriggerName.IDLENESS, confs);
  }

  Map<ProcessPath, Float> ratios() {
    final var totalWavedUnits = distribution.values().stream().reduce(0, Integer::sum).floatValue();

    return distribution.entrySet()
        .stream()
        .collect(
            toMap(Map.Entry::getKey, entry -> safeDiv(entry.getValue(), totalWavedUnits))
        );
  }

  WaveDistribution applyAdjustment(
      final Map<ProcessPath, Integer> unitsDiff,
      final Map<ProcessPath, Integer> maxWaveableUnits
  ) {
    final var adjustedDistribution = distribution.keySet()
        .stream()
        .collect(
            toMap(
                Function.identity(),
                pp -> Math.max(
                    lowerBounds.get(pp),
                    Math.min(
                        distribution.get(pp) + unitsDiff.getOrDefault(pp, 0),
                        maxWaveableUnits.get(pp)
                    )
                )
            )
        );

    return new WaveDistribution(adjustedDistribution, lowerBounds);
  }
}
