package com.mercadolibre.planning.model.api.projection.waverless;

import static com.mercadolibre.planning.model.api.projection.waverless.PickingProjectionBuilder.asPiecewiseUpstream;
import static com.mercadolibre.planning.model.api.projection.waverless.PickingProjectionBuilder.backlogProjection;
import static com.mercadolibre.planning.model.api.projection.waverless.PickingProjectionBuilder.buildContextHolder;

import com.mercadolibre.flow.projection.tools.services.entities.context.PiecewiseUpstream;
import com.mercadolibre.flow.projection.tools.services.entities.process.Processor;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.waverless.PickingProjectionBuilder.ProcessPathBacklog;
import com.mercadolibre.planning.model.api.projection.waverless.SlaWaveCalculator.CurrentBacklog;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class NextSlaWaveProjector {

  private static final int MIN_INFLECTION_POINTS_TO_PROJECT = 2;

  private static final long INFLECTION_POINT_WINDOW_SIZE = 5L;

  private NextSlaWaveProjector() {
  }

  /**
   * Projects the states of the picking backlog and returns the next wave by sla.
   *
   * <p>Calculates pickings' backlogs states by applying the projected waves.
   * With the resulting state and the next wave by sla is calculated.
   *
   * @param inflectionPoints dates that will be projected
   * @param waves            existing waves
   * @param pendingBacklog   forecasted backlog by Process Path.
   * @param currentBacklog   initial picking backlogs by process path
   * @param throughput       throughput by Process Path and Hour.
   * @param minCycleTimes    minimum cycle time configuration by Process Path. It must contain all Process Paths.
   * @return next wave configuration, if found.
   */
  public static Optional<Wave> nextWave(
      final List<Instant> inflectionPoints,
      final List<Wave> waves,
      final PendingBacklog pendingBacklog,
      final Map<ProcessPath, Map<Instant, Integer>> currentBacklog,
      final Map<ProcessPath, Map<Instant, Integer>> throughput,
      final Map<ProcessPath, Integer> minCycleTimes
  ) {
    final var nextWaveCandidateInflectionPoints = slaProjectionInflectionPoints(inflectionPoints, waves);
    if (nextWaveCandidateInflectionPoints.size() <= MIN_INFLECTION_POINTS_TO_PROJECT) {
      return Optional.empty();
    }

    final var projectedBacklogs = calculateBacklogStates(inflectionPoints, currentBacklog, throughput, waves);

    return calculateNextWaveWithBacklog(
        nextWaveCandidateInflectionPoints,
        waves,
        projectedBacklogs,
        throughput,
        pendingBacklog,
        minCycleTimes
    );
  }

  private static List<Instant> slaProjectionInflectionPoints(final List<Instant> inflectionPoints, final List<Wave> waves) {
    return waves.stream()
        .map(Wave::getDate)
        .max(Comparator.naturalOrder())
        .map(lastWaveDate -> inflectionPoints.stream()
            .filter(ip -> ip.isAfter(lastWaveDate))
            .sorted()
            .collect(Collectors.toList())
        )
        .orElse(inflectionPoints);
  }

  private static Optional<Wave> calculateNextWaveWithBacklog(
      final List<Instant> inflectionPoints,
      final List<Wave> waves,
      final Stream<ProcessPathBacklog> projectedBacklogs,
      final Map<ProcessPath, Map<Instant, Integer>> throughput,
      final PendingBacklog pendingBacklog,
      final Map<ProcessPath, Integer> minCycleTimes
  ) {
    final var backlogProjection = projectedBacklogs.collect(Collectors.groupingBy(
            ProcessPathBacklog::getDate,
            Collectors.mapping(NextSlaWaveProjector::toCurrentBacklog, Collectors.toList())
        ));

    return SlaWaveCalculator.projectNextWave(
        inflectionPoints,
        backlogProjection,
        throughput,
        pendingBacklog,
        minCycleTimes,
        waves
    );
  }

  private static CurrentBacklog toCurrentBacklog(final ProcessPathBacklog projection) {
    return new CurrentBacklog(projection.getProcessPath(), projection.getCpt(), projection.getUnits().intValue());
  }

  private static Stream<ProcessPathBacklog> calculateBacklogStates(
      final List<Instant> inflectionPoints,
      final Map<ProcessPath, Map<Instant, Integer>> currentBacklog,
      final Map<ProcessPath, Map<Instant, Integer>> throughput,
      final List<Wave> waves
  ) {
    final var graph = PickingProjectionBuilder.buildGraph(new ArrayList<>(throughput.keySet()));

    final var currentInflectionPoint = inflectionPoints.get(0);
    return Stream.concat(
        mapCurrentBacklogState(currentInflectionPoint, currentBacklog),
        projectFutureBacklogStates(graph, currentInflectionPoint, inflectionPoints, currentBacklog, throughput, waves)
    );
  }

  private static Stream<ProcessPathBacklog> mapCurrentBacklogState(
      final Instant currentInflectionPoint,
      final Map<ProcessPath, Map<Instant, Integer>> currentBacklog
  ) {
    return currentBacklog.keySet()
        .stream()
        .flatMap(pp -> currentBacklog.get(pp).entrySet()
            .stream()
            .map(entry -> new ProcessPathBacklog(
                    currentInflectionPoint,
                    pp,
                    ProcessName.PICKING,
                    entry.getKey(),
                    entry.getValue().longValue()
                )
            )
        );
  }

  private static Stream<ProcessPathBacklog> projectFutureBacklogStates(
      final Processor graph,
      final Instant currentInflectionPoint,
      final List<Instant> inflectionPoints,
      final Map<ProcessPath, Map<Instant, Integer>> currentBacklog,
      final Map<ProcessPath, Map<Instant, Integer>> throughput,
      final List<Wave> waves
  ) {
    final var contextsHolder = buildContextHolder(currentBacklog, throughput);
    final var upstream = toPiecewiseUpstream(waves);
    return backlogProjection(graph, contextsHolder, upstream, inflectionPoints, throughput.keySet()).stream()
        .filter(projection -> !projection.getDate().equals(currentInflectionPoint));
  }

  private static PiecewiseUpstream toPiecewiseUpstream(final List<Wave> waves) {
    final var wavesByDate = waves.stream()
        .collect(Collectors.toMap(
            Wave::getDate,
            wave -> wave.getConfiguration().entrySet()
                .stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().getWavedUnitsByCpt()
                ))
        ));

    final var emptyWave = Map.of(ProcessPath.TOT_MONO, Map.of(Instant.now(), 0L));

    // TODO: replace this when updating lib upstream backlog to an interface
    final var fixedWaves = new HashMap<>(wavesByDate);
    wavesByDate.forEach((date, wave) -> fixedWaves.put(date.plus(INFLECTION_POINT_WINDOW_SIZE, ChronoUnit.MINUTES), emptyWave));

    return asPiecewiseUpstream(fixedWaves);
  }

}
