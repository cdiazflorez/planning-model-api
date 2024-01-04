package com.mercadolibre.planning.model.api.projection.waverless.idleness;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.TriggerName.IDLENESS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.BacklogProjection;
import com.mercadolibre.planning.model.api.projection.waverless.PendingBacklog;
import com.mercadolibre.planning.model.api.projection.waverless.PrecalculatedWave;
import com.mercadolibre.planning.model.api.projection.waverless.Wave;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class NextIdlenessWaveProjector {

  private static final String WH_ARBA = "ARBA01";
  private static final String WH_BRBA = "BRBA01";
  private static final String WH_MXYU = "MXYU01";
  private static final Map<String, Integer> MIN_TPH_IN_MINUTES_BY_WAREHOUSE = Map.of(WH_ARBA, 60, WH_BRBA, 45, WH_MXYU, 30);
  private static final Map<String, Integer> MAX_TPH_IN_MINUTES_BY_WAREHOUSE = Map.of(WH_ARBA, 90, WH_BRBA, 75, WH_MXYU, 45);
  private static final Map<String, Integer> MIN_TPH_IN_MINUTES_BY_WAREHOUSE_TRIGGER = Map.of(WH_ARBA, 30, WH_BRBA, 30, WH_MXYU, 30);
  private static final int MINUTES_IN_HOUR = 60;

  private NextIdlenessWaveProjector() {

  }

  /**
   * Calculates the next idleness wave.
   *
   * <p>Calculates if Picking's backlogs will be under its limits, by making a projection and comparing the projected backlogs with the
   * configured limits. If it is projected that the backlogs will be under its limits then a wave is configured by obtaining the
   * lower bounds, upper bounds and units to wave by Process Path.
   *
   * @param logisticCenterId   logistic center.
   * @param inflectionPoints   points to time in which backlogs must be evaluated.
   * @param pendingBacklog     backlog in ready to wave and forecasted.
   * @param backlogs           current backlogs by process and process path and cpt.
   * @param throughput         tph by process and process path.
   * @param precalculatedWaves precalculated wave distributions by process path.
   * @param previousWaves      previously calculated waves.
   * @return if found, a wave for idleness.
   */
  @Trace
  public static Optional<DateWaveSupplier> calculateNextWave(
      final String logisticCenterId,
      final List<Instant> inflectionPoints,
      final PendingBacklog pendingBacklog,
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> backlogs,
      final Map<ProcessPath, Map<ProcessName, Map<Instant, Integer>>> throughput,
      final Map<ProcessPath, List<PrecalculatedWave>> precalculatedWaves,
      final List<Wave> previousWaves
  ) {
    final var backlogsStates = calculateBacklogStates(inflectionPoints, backlogs, throughput, previousWaves);
    final var lowerBoundInMinutes = MIN_TPH_IN_MINUTES_BY_WAREHOUSE.getOrDefault(logisticCenterId, 30);
    final var upperBoundInMinutes = MAX_TPH_IN_MINUTES_BY_WAREHOUSE.getOrDefault(logisticCenterId, 90);
    final var minimumBacklog = getMinimumBacklog(
        throughput.get(ProcessPath.GLOBAL).get(PICKING),
        MIN_TPH_IN_MINUTES_BY_WAREHOUSE_TRIGGER.getOrDefault(logisticCenterId, 30)
    );
    final var pickingBacklogs = backlogsStates.get(PICKING);
    final var waveDate = calculateWaveDate(minimumBacklog, pickingBacklogs);

    return waveDate.filter(date -> isAfterPreviousWavesByIdleness(date, previousWaves))
        .map(date ->
            new DateWaveSupplier(
                date,
                () -> new Wave(date,
                    IDLENESS,
                    getConfigurations(
                        date,
                        previousWaves,
                        pendingBacklog,
                        lowerBoundInMinutes,
                        upperBoundInMinutes,
                        throughput,
                        precalculatedWaves
                    )
                )
            ));
  }

  public static Map<ProcessName, Map<Instant, Long>> calculateBacklogStates(
      final List<Instant> inflectionPoints,
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> backlogs,
      final Map<ProcessPath, Map<ProcessName, Map<Instant, Integer>>> throughput,
      final List<Wave> previousWaves
  ) {
    final var globalThroughput = throughput.get(ProcessPath.GLOBAL);

    return Stream.of(
            buildCurrentBacklogs(inflectionPoints.get(0), backlogs),
            BacklogProjection.project(inflectionPoints, previousWaves, backlogs, globalThroughput)
        )
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .collect(
            groupingBy(
                Map.Entry::getKey,
                flatMapping(
                    entry -> entry.getValue().entrySet().stream(),
                    toMap(Map.Entry::getKey, Map.Entry::getValue)
                )
            )
        );
  }

  private static Map<ProcessName, Map<Instant, Long>> buildCurrentBacklogs(
      final Instant date,
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> backlogs
  ) {
    return backlogs.entrySet()
        .stream()
        .collect(
            toMap(
                Map.Entry::getKey,
                entry -> Map.of(
                    date,
                    entry.getValue()
                        .values()
                        .stream()
                        .map(Map::values)
                        .flatMap(Collection::stream)
                        .reduce(0L, Long::sum)
                )
            )
        );
  }

  private static Map<Instant, Integer> getMinimumBacklog(final Map<Instant, Integer> throughput, int tphTime) {
    return throughput.entrySet()
        .stream()
        .collect(toMap(
            Map.Entry::getKey,
            entry -> (entry.getValue() * tphTime) / MINUTES_IN_HOUR
        ));
  }

  private static Optional<Instant> calculateWaveDate(
      final Map<Instant, Integer> minimumBacklog,
      final Map<Instant, Long> pickingBacklogs
  ) {
    final var minWaveDate = pickingBacklogs.keySet()
        .stream()
        .min(Comparator.naturalOrder())
        .orElseThrow();

    final var idlenessDate = pickingBacklogs.entrySet()
        .stream()
        .filter(backlog -> backlog.getValue() < minimumBacklog.get(backlog.getKey().truncatedTo(HOURS)))
        .map(Map.Entry::getKey)
        .filter(date -> date.isAfter(minWaveDate))
        .min(Comparator.naturalOrder());

    return idlenessDate.map(id -> pickingBacklogs.keySet()
        .stream()
        .filter(date -> date.isBefore(id))
        .max(Comparator.naturalOrder())
        .orElse(minWaveDate)
    );
  }

  private static boolean isAfterPreviousWavesByIdleness(final Instant waveDate, final List<Wave> previousWaves) {
    final var maxWaveDate = previousWaves.stream()
        .filter(wave -> wave.getReason() == IDLENESS)
        .map(Wave::getDate)
        .max(Comparator.naturalOrder());

    if (maxWaveDate.isEmpty()) {
      return true;
    }

    final var previousWaveDate = maxWaveDate.get();
    final var isAfter = waveDate.isAfter(previousWaveDate);

    if (!isAfter) {
      log.error("calculated idleness wave date is not after the previous one. new: {}, previous: {}", waveDate, previousWaveDate);
    }

    return isAfter;
  }

  private static Map<ProcessPath, Wave.WaveConfiguration> getConfigurations(
      final Instant waveExecutionDate,
      final List<Wave> previousWaves,
      final PendingBacklog pendingBacklog,
      final int lowerBoundInMinutes,
      final int upperBoundInMinutes,
      final Map<ProcessPath, Map<ProcessName, Map<Instant, Integer>>> throughputByProcessPath,
      final Map<ProcessPath, List<PrecalculatedWave>> precalculatedWaves
  ) {

    final var pickingThroughput = getPickingThroughputByProcessPath(throughputByProcessPath);

    final var lowerBounds = BoundsCalculator.execute(
        lowerBoundInMinutes,
        waveExecutionDate,
        pickingThroughput
    );

    final var upperBounds = BoundsCalculator.execute(
        upperBoundInMinutes,
        waveExecutionDate,
        pickingThroughput
    );

    final var wavedUnitsByCptAndProcessPath = UnitsByCptCalculator.calculateBacklogToWave(
        waveExecutionDate,
        previousWaves,
        pendingBacklog,
        upperBounds,
        lowerBounds,
        precalculatedWaves
    );

    return throughputByProcessPath.keySet()
        .stream()
        .filter(pp -> pp != ProcessPath.GLOBAL)
        .collect(
            toMap(
                Function.identity(),
                pp -> new Wave.WaveConfiguration(
                    lowerBounds.get(pp),
                    Math.max(lowerBounds.get(pp), upperBounds.get(pp)),
                    wavedUnitsByCptAndProcessPath.get(pp)
                )
            )
        );
  }

  private static Map<ProcessPath, Map<Instant, Integer>> getPickingThroughputByProcessPath(
      final Map<ProcessPath, Map<ProcessName, Map<Instant, Integer>>> throughputByProcessPath
  ) {
    return throughputByProcessPath.entrySet()
        .stream()
        .filter(entry -> entry.getKey() != ProcessPath.GLOBAL)
        .collect(toMap(Map.Entry::getKey, entry -> entry.getValue().get(PICKING)));
  }
}
