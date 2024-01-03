package com.mercadolibre.planning.model.api.projection.waverless.idleness;

import static com.mercadolibre.planning.model.api.domain.entity.TriggerName.IDLENESS;
import static com.mercadolibre.planning.model.api.domain.entity.TriggerName.SLA;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.waverless.PendingBacklog;
import com.mercadolibre.planning.model.api.projection.waverless.PrecalculatedWave;
import com.mercadolibre.planning.model.api.projection.waverless.Wave;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

final class UnitsByCptCalculator {
  private UnitsByCptCalculator() {

  }

  /**
   * Given a wave configuration and the previously calculated waves, calculates the units to wave by cpt for each Process Path.
   *
   * <p>Calculates the {@link Wave.WaveConfiguration#getWavedUnitsByCpt()} configuration parameter that defines how many units of each
   * Process Path and cpt are waved. To calculate the distribution of units by cpt for the n-th suggestion for idleness,
   * it tries to use the value defined by the n-th precalculated wave -possibly resizing it if it is outside the bounds of the wave-.
   * If there is not a precalculated wave, then the default strategy of waving as many units as the upper bound in FEFO order is applied.
   *
   * <p>Last, the calculated distribution is capped by the available backlog. As it would be an error trying to wave more units than
   * those that are available.
   *
   * @param waveDate           wave execution date.
   * @param previousWaves      previously calculated waves.
   * @param pendingBacklog     backlog in ready to wave and forecasted.
   * @param upperBounds        upper wave bounds by process path.
   * @param lowerBounds        lower wave bounds by process path.
   * @param precalculatedWaves precalculated wave distributions by process path.
   * @return wave distribution by process path and cpt inside wave bounds.
   */
  static Map<ProcessPath, Map<Instant, Long>> calculateBacklogToWave(
      final Instant waveDate,
      final List<Wave> previousWaves,
      final PendingBacklog pendingBacklog,
      final Map<ProcessPath, Integer> upperBounds,
      final Map<ProcessPath, Integer> lowerBounds,
      final Map<ProcessPath, List<PrecalculatedWave>> precalculatedWaves
  ) {
    // max backlog to wave by process path and cpt
    final var paths = new ArrayList<>(upperBounds.keySet());
    final var availableBacklog = pendingBacklog.availableBacklogAt(waveDate, paths, previousWaves);
    final var cptWavesSla = previousWaves.stream()
        .filter(m -> m.getReason() == SLA)
        .map(Wave::getConfiguration)
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .collect(toMap(
            Map.Entry::getKey,
            m -> m.getValue().getWavedUnitsByCpt().keySet(),
            (o, n) -> {
              var join = new HashSet<>(o);
              join.addAll(n);
              return join;
            }));
    final var currentWaveIndex = (int) previousWaves.stream().filter(wave -> wave.getReason() == IDLENESS).count();

    return paths.stream()
        .collect(
            toMap(
                Function.identity(),
                pp -> getWaveToApply(
                    currentWaveIndex,
                    precalculatedWaves.get(pp),
                    upperBounds.get(pp),
                    lowerBounds.get(pp),
                    availableBacklog.get(pp),
                    cptWavesSla.getOrDefault(pp, Set.of())
                )
            )
        );
  }

  private static Map<Instant, Long> getWaveToApply(
      final int currentWaveIndex,
      final List<PrecalculatedWave> waves,
      final int upperBound,
      final int lowerBound,
      final Map<Instant, Long> availableBacklog,
      final Set<Instant> cptWavesSla
  ) {
    final Optional<PrecalculatedWave> distribution = Optional.ofNullable(waves)
        .filter(pw -> currentWaveIndex < pw.size())
        .map(pw -> pw.get(currentWaveIndex));

    return distribution.map(wave -> calculateApplicableBacklogToWave(wave, upperBound, lowerBound, cptWavesSla))
        .map(wave -> limitWavedUnitsByCptWithAvailableBacklog(wave, availableBacklog))
        .orElseGet(() -> splitBacklogToWaveBySla(upperBound, availableBacklog));
  }

  private static Map<Instant, Long> calculateApplicableBacklogToWave(
      final PrecalculatedWave wave,
      final Integer upperBound,
      final Integer lowerBound,
      final Set<Instant> cptWavesSla
  ) {
    final var distribution = wave.getUnitsBySla()
        .entrySet()
        .stream()
        .filter(m -> !cptWavesSla.contains(m.getKey()))
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    final var wavedUnits = distribution.values().stream().reduce(0L, Long::sum);

    if (lowerBound <= wavedUnits && wavedUnits <= upperBound) {
      return distribution;
    } else {
      final var diff = wavedUnits < lowerBound
          ? lowerBound - wavedUnits
          : upperBound - wavedUnits;

      return resizePrecalculatedWaveDistribution(distribution, wavedUnits, diff);
    }
  }

  private static Map<Instant, Long> limitWavedUnitsByCptWithAvailableBacklog(
      final Map<Instant, Long> externalDesiredBacklogToWave,
      final Map<Instant, Long> availableBacklog
  ) {

    final var backlogByCpt = externalDesiredBacklogToWave.entrySet()
        .stream()
        .collect(
            toMap(
                Map.Entry::getKey,
                entry -> Math.min(entry.getValue(), availableBacklog.getOrDefault(entry.getKey(), 0L))
            )
        );

    final long totalBacklogToWave = externalDesiredBacklogToWave.values().stream().reduce(0L, Long::sum);
    final long totalBacklogByCpt = backlogByCpt.values().stream().reduce(0L, Long::sum);
    long diff = totalBacklogToWave - totalBacklogByCpt;

    for (Instant sla : new TreeSet<>(availableBacklog.keySet())) {
      if (diff <= 0) {
        break;
      }

      final long remainingSlaBacklog = availableBacklog.get(sla) - backlogByCpt.getOrDefault(sla, 0L);
      if (remainingSlaBacklog <= 0) {
        continue;
      }
      final long unitsToAdd = Math.min(remainingSlaBacklog, diff);
      backlogByCpt.put(sla, backlogByCpt.getOrDefault(sla, 0L) + unitsToAdd);
      diff -= unitsToAdd;
    }

    return backlogByCpt;
  }

  private static Map<Instant, Long> resizePrecalculatedWaveDistribution(final Map<Instant, Long> wave, final long wavedUnits,
                                                                        final long diff) {
    return wave.entrySet()
        .stream()
        .collect(
            toMap(
                Map.Entry::getKey,
                entry -> (long) (entry.getValue() + ((float) entry.getValue() / (float) wavedUnits) * diff)
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

}
