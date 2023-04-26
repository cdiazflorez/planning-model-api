package com.mercadolibre.planning.model.api.projection.waverless.idleness;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.projection.waverless.PickingProjectionBuilder.asPiecewiseUpstream;
import static com.mercadolibre.planning.model.api.projection.waverless.idleness.BacklogProjection.buildContexts;
import static com.mercadolibre.planning.model.api.projection.waverless.idleness.BacklogProjection.buildGraph;
import static com.mercadolibre.planning.model.api.projection.waverless.idleness.BacklogProjection.project;
import static com.mercadolibre.planning.model.api.util.MathUtil.safeDiv;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.mercadolibre.flow.projection.tools.services.entities.context.PiecewiseUpstream;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessNameToProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.waverless.PendingBacklog;
import com.mercadolibre.planning.model.api.projection.waverless.Wave;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class UpperBoundsCalculator {

  private static final int MAX_OPTIMIZATION_STEPS = 50;

  private static final long INFLECTION_POINT_WINDOW_SIZE = 5L;

  private static final Set<ProcessName> PROCESSES = Set.of(PICKING, PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL);

  private UpperBoundsCalculator() {
  }

  /**
   * Calculates idleness wave upper bound.
   *
   * <p>Given the current backlog projection, a starting wave distribution is generated
   * by invoking the selected {@link InitialWaveStrategy}. This wave is used as a starting point for the optimization
   * as it is the biggest possible wave that does not break through the Picking upper limit.
   *
   * <p>With the first wave an acceptable wave distribution is found by an iterative process that makes a projection and applies an
   * adjustment based on the differences between the projected backlogs and the backlog limits. This adjustment might prune some process
   * path -a negative adjustment- or assigned more waving capacity -a positive adjustment-. With the adjusted distributions another step
   * of the iteration is executed, as a change in the distribution will produce a change in the throughput assignment by Process Path
   * and this might imply another adjustment.
   *
   * <p>At most {@link #MAX_OPTIMIZATION_STEPS} are executed but the optimization process can finish before if it finds an optimal solution
   * -the adjustment is zero for all process paths- or if applying an adjustment does not produce a change in the wave distribution -the
   * adjustment can not be applied by a limitation in the available backlog-.
   *
   * @param inflectionPoints               points in time in which a backlog projection must be evaluated.
   * @param waveExecutionDate              date in which backlog must be feed to picking in order to avoid idleness.
   * @param previousWaves                  waves that must be applied before waveExecutionDate.
   * @param currentBacklog                 initial backlog by process and process path.
   * @param projectedBacklog               backlog projection with previous waves applied.
   * @param throughputByProcess            available global throughput by process.
   * @param pickingThroughputByProcessPath available picking throughput by process path.
   * @param backlogUpperLimits             max backlog by process and date.
   * @return max backlog to wave by Process Path.
   */
  static Map<ProcessPath, Integer> calculate(
      final Instant waveExecutionDate,
      final List<Instant> inflectionPoints,
      final List<Wave> previousWaves,
      final PendingBacklog pendingBacklog,
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentBacklog,
      final List<BacklogQuantityAtInflectionPoint> projectedBacklog,
      final Map<ProcessName, Map<Instant, Integer>> throughputByProcess,
      final Map<ProcessPath, Map<Instant, Integer>> pickingThroughputByProcessPath,
      final Map<ProcessName, Map<Instant, Integer>> backlogUpperLimits
  ) {
    final var processPaths = new ArrayList<>(pickingThroughputByProcessPath.keySet());

    final var maxWaveableUnits = calculateMaxWaveableUnits(waveExecutionDate, previousWaves, pendingBacklog, processPaths);

    var waveDistribution = calculateInitialWaveDistribution(
        waveExecutionDate,
        projectedBacklog,
        pickingThroughputByProcessPath,
        backlogUpperLimits
    );

    for (int i = 0; i < MAX_OPTIMIZATION_STEPS; i++) {
      final var wave = waveDistribution.asWave(waveExecutionDate, pendingBacklog, previousWaves);

      final var adjustment = waveAdjustment(
          wave,
          waveExecutionDate,
          waveDistribution,
          previousWaves,
          inflectionPoints,
          currentBacklog,
          throughputByProcess,
          backlogUpperLimits
      );

      if (totalAdjustedUnits(adjustment) == 0) {
        return waveDistribution.getDistribution();
      }

      final var adjustedWaveDistribution = waveDistribution.applyAdjustment(adjustment, maxWaveableUnits);

      if (adjustedWaveDistribution.equals(waveDistribution)) {
        return waveDistribution.getDistribution();
      }

      waveDistribution = adjustedWaveDistribution;
    }
    return waveDistribution.getDistribution();
  }

  private static WaveDistribution calculateInitialWaveDistribution(
      final Instant waveExecutionDate,
      final List<BacklogQuantityAtInflectionPoint> projectedBacklog,
      final Map<ProcessPath, Map<Instant, Integer>> pickingThroughputByProcessPath,
      final Map<ProcessName, Map<Instant, Integer>> backlogUpperLimits
  ) {
    final var projectionsByDate = groupProjectionsByDate(projectedBacklog);

    final var waveExecutionHour = waveExecutionDate.truncatedTo(HOURS);

    final var bufferSize = backlogUpperLimits.get(PICKING).get(waveExecutionHour) - projectionsByDate.get(PICKING).get(waveExecutionDate);

    return InitialWaveStrategy.matchThroughputRatio(
        waveExecutionDate,
        bufferSize,
        pickingThroughputByProcessPath
    );
  }

  private static Map<ProcessName, Map<Instant, Long>> groupProjectionsByDate(final List<BacklogQuantityAtInflectionPoint> projections) {
    return projections.stream()
        .collect(
            groupingBy(
                BacklogQuantityAtInflectionPoint::getProcessName,
                toMap(BacklogQuantityAtInflectionPoint::getInflectionPoint, BacklogQuantityAtInflectionPoint::getQuantity, Long::sum)
            )
        );
  }

  private static Map<ProcessPath, Integer> calculateMaxWaveableUnits(
      final Instant waveDate,
      final List<Wave> previousWaves,
      final PendingBacklog pendingBacklog,
      final List<ProcessPath> processPaths
  ) {
    final var availableBacklog = pendingBacklog.availableBacklogAt(waveDate, processPaths, previousWaves);
    return availableBacklog.entrySet()
        .stream()
        .collect(
            toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().values().stream().mapToInt(Long::intValue).sum(), Integer::sum
            )
        );
  }

  private static Map<ProcessPath, Integer> waveAdjustment(
      final Wave candidateWave,
      final Instant waveExecutionDate,
      final WaveDistribution waveDistribution,
      final List<Wave> previousWaves,
      final List<Instant> inflectionPoints,
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentBacklog,
      final Map<ProcessName, Map<Instant, Integer>> throughput,
      final Map<ProcessName, Map<Instant, Integer>> upperBacklogLimits
  ) {
    final var projections = applyWave(candidateWave, waveExecutionDate, previousWaves, inflectionPoints, currentBacklog, throughput);
    final var minDiffBetweenTargetAndProjectedBacklogByProcess = calculateBufferSizeByProcess(projections, upperBacklogLimits);
    return diffByProcessPath(minDiffBetweenTargetAndProjectedBacklogByProcess, waveDistribution.ratios());
  }

  private static Map<ProcessName, Map<Instant, Long>> applyWave(
      final Wave candidateWave,
      final Instant waveExecutionDate,
      final List<Wave> previousWaves,
      final List<Instant> inflectionPoints,
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentBacklog,
      final Map<ProcessName, Map<Instant, Integer>> throughput
  ) {
    final var allWaves = new ArrayList<>(previousWaves);
    allWaves.add(candidateWave);

    final var projection = getBacklogProjection(inflectionPoints, allWaves, currentBacklog, throughput);
    final var filteredProjections = projection.stream()
        .filter(bp -> bp.getInflectionPoint().isAfter(waveExecutionDate) || bp.getInflectionPoint().equals(waveExecutionDate))
        .collect(Collectors.toList());

    return groupProjectionsByDate(filteredProjections);
  }

  private static Map<ProcessPath, Integer> diffByProcessPath(
      final Map<ProcessName, Integer> buffers,
      final Map<ProcessPath, Float> waveRatios
  ) {
    final var processPaths = waveRatios.keySet();
    final Map<ProcessName, Map<ProcessPath, Float>> adjustmentsByProcessAndPath = new EnumMap<>(ProcessName.class);

    final Integer pickingBufferSize = buffers.get(PICKING);
    final Map<ProcessPath, Float> pickingAdjustment = waveRatios.entrySet().stream().collect(toMap(
        Map.Entry::getKey,
        entry -> entry.getValue() * pickingBufferSize
    ));

    adjustmentsByProcessAndPath.put(PICKING, pickingAdjustment);

    for (ProcessNameToProcessPath processNameToProcessPath : ProcessNameToProcessPath.getTriggersProcess()) {
      final ProcessName process = processNameToProcessPath.getProcess();

      final Integer processBufferSize = buffers.get(process);

      final ProcessName previousProcess = process.getPreviousProcesses();
      final Map<ProcessPath, Float> previousProcessContext = adjustmentsByProcessAndPath.get(previousProcess);

      final var paths = processNameToProcessPath.getPaths()
          .stream()
          .filter(processPaths::contains)
          .collect(toSet());

      final var dist = bufferDistributionByProcessPath(paths, previousProcessContext, processBufferSize);

      adjustmentsByProcessAndPath.put(process, dist);
    }

    return calculateMinBuffDiffByProcessPath(adjustmentsByProcessAndPath);
  }

  private static Map<ProcessPath, Float> bufferDistributionByProcessPath(
      final Set<ProcessPath> processPaths,
      final Map<ProcessPath, Float> previousProcessContext,
      final Integer processPathAvailableCapacity
  ) {
    final float total = processPaths.stream()
        .map(previousProcessContext::get)
        .filter(Objects::nonNull)
        .reduce(0F, Float::sum);

    return processPaths.stream()
        .collect(
            toMap(
                Function.identity(),
                pp -> safeDiv(previousProcessContext.get(pp), total) * processPathAvailableCapacity
            )
        );
  }

  private static Map<ProcessPath, Integer> calculateMinBuffDiffByProcessPath(
      final Map<ProcessName, Map<ProcessPath, Float>> diffsByProcessAndProcessPath
  ) {
    return diffsByProcessAndProcessPath.values().stream()
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .collect(
            toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().intValue(),
                Integer::min
            )
        );
  }

  private static Map<ProcessName, Integer> calculateBufferSizeByProcess(
      final Map<ProcessName, Map<Instant, Long>> backlogs,
      final Map<ProcessName, Map<Instant, Integer>> upperBacklogLimits
  ) {
    return backlogs.keySet()
        .stream()
        .collect(
            toMap(
                Function.identity(),
                process -> reduceBufferSizeByProcess(
                    backlogs.get(process),
                    upperBacklogLimits.get(process)
                )
            )
        );
  }

  private static Integer reduceBufferSizeByProcess(
      final Map<Instant, Long> backlogs,
      final Map<Instant, Integer> upperBacklogLimits
  ) {
    // buffer size can be negative
    return backlogs.entrySet()
        .stream()
        .map(entry -> upperBacklogLimits.get(entry.getKey().truncatedTo(HOURS)) - entry.getValue())
        .min(Comparator.naturalOrder())
        .map(Long::intValue)
        .orElse(0);
  }

  private static List<BacklogQuantityAtInflectionPoint> getBacklogProjection(
      final List<Instant> inflectionPoints,
      final List<Wave> waves,
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentBacklog,
      final Map<ProcessName, Map<Instant, Integer>> throughput
  ) {
    final var graph = buildGraph();
    final var contexts = buildContexts(currentBacklog, throughput);
    final var upstream = toPiecewiseUpstream(waves);

    return project(graph, contexts, upstream, inflectionPoints, PROCESSES);
  }

  private static PiecewiseUpstream toPiecewiseUpstream(final List<Wave> waves) {
    final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> wavesByDate = waves.stream()
        .collect(toMap(
            Wave::getDate,
            wave -> wave.getConfiguration().entrySet()
                .stream()
                .collect(toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().getWavedUnitsByCpt()
                ))
        ));

    final var emptyWave = Map.of(ProcessPath.TOT_MONO, Map.of(Instant.now(), 0L));

    // TODO: replace this when updating lib upstream backlog to an interface
    final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> fixedWaves = new HashMap<>();
    wavesByDate.forEach((date, wave) -> fixedWaves.put(date.plus(INFLECTION_POINT_WINDOW_SIZE, ChronoUnit.MINUTES), emptyWave));
    fixedWaves.putAll(wavesByDate);

    return asPiecewiseUpstream(fixedWaves);
  }


  private static int totalAdjustedUnits(final Map<ProcessPath, Integer> unitsDiff) {
    return unitsDiff.values()
        .stream()
        .map(Math::abs)
        .reduce(0, Integer::sum);
  }

}
