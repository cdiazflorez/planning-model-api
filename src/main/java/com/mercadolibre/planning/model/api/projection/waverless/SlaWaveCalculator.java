package com.mercadolibre.planning.model.api.projection.waverless;

import static com.mercadolibre.planning.model.api.domain.entity.TriggerName.SLA;
import static com.mercadolibre.planning.model.api.projection.waverless.PickingProjectionBuilder.buildContextHolder;
import static com.mercadolibre.planning.model.api.projection.waverless.PickingProjectionBuilder.buildGraph;
import static com.mercadolibre.planning.model.api.projection.waverless.PickingProjectionBuilder.projectSla;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import com.mercadolibre.flow.projection.tools.services.entities.process.Processor;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.waverless.idleness.DateWaveSupplier;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Value;

public final class SlaWaveCalculator {

  private static final String WH = "ARBA01";

  private SlaWaveCalculator() {
  }

  /**
   * Calculates the next wave by sla.
   *
   * <p>Calculates an SLA projection for each inflection point simulating that all the available backlog that will be in ready to wave
   * at that inflection point is waved. If any of the projected SLAs is finished after it's deadline date (SLA - minCycleTime) then
   * that SLA must be waved in the prior inflection point, and the wave must contain that SLA and all those that have a greater priority.
   *
   * <p>Returns the composition of waved backlog by SLA so that it can be removed from the backlog
   * in ready to wave before the next execution.
   *
   * <p>Only the first wave is calculated as any of the following waves that could be calculated might be invalidated by an idleness wave.
   *
   * <p>This is an unoptimized implementation as the inflection point could be searched with a binary search between the current date and
   * the earliest SLA date.
   *
   * @param inflectionPoints  dates for which the SLA will be verified.
   * @param projectedBacklogs projected backlog in ready to pick by Projection Date, Process Path, and Sla.
   * @param throughput        throughput by Process Path and Hour.
   * @param pendingBacklog    forecasted backlog by Process Path.
   * @param minCycleTimes     minimum cycle time configuration by Process Path. It must contain all Process Paths.
   * @param waves             existing waves
   * @return next wave configuration, if found.
   */
  @Trace
  public static Optional<DateWaveSupplier> projectNextWave(
      final List<Instant> inflectionPoints,
      final Map<Instant, List<CurrentBacklog>> projectedBacklogs,
      final Map<ProcessPath, Map<Instant, Integer>> throughput,
      final PendingBacklog pendingBacklog,
      final Map<ProcessPath, Integer> minCycleTimes,
      final List<Wave> waves,
      final String logisticCenterId
  ) {
    final var slas = pendingBacklog.calculateSlasByProcessPath();

    final var deadlines = calculateDeadlines(slas, minCycleTimes);
    final var pessimisticDeadlines = pessimisticCalculateDeadlines(slas, minCycleTimes);

    final var processPaths = new ArrayList<>(throughput.keySet());
    final var graph = buildGraph(processPaths);

    final var simulationContext = new SimulationContext(
        WH.equals(logisticCenterId) ? pessimisticDeadlines : deadlines,
        processPaths,
        graph,
        inflectionPoints,
        projectedBacklogs,
        throughput,
        pendingBacklog
    );

    Optional<Map<ProcessPath, Map<Instant, Long>>> previousWavedBacklog = Optional.empty();
    Instant lastInflectionPoint = inflectionPoints.get(0);
    final List<Instant> inflectionPointsToProject = inflectionPoints.subList(0, inflectionPoints.size() - 1);
    for (Instant inflectionPoint : inflectionPointsToProject) {
      final var backlogToWave = backlogToWave(inflectionPoint, pendingBacklog, waves, processPaths, deadlines);

      final var expiredSlas = calculateSlaExpirationWithWaveSimulation(inflectionPoint, simulationContext, backlogToWave);
      final var wavedBacklog = previousWavedBacklog.orElse(backlogToWave);

      final var wave = buildWaveFromExpiredSlas(lastInflectionPoint, wavedBacklog, expiredSlas);
      if (wave.isPresent()) {
        ExecutionMetrics.SLA_DATE_ITERATIONS.count(inflectionPointsToProject.indexOf(inflectionPoint));
        return wave;
      }

      previousWavedBacklog = Optional.of(backlogToWave);
      lastInflectionPoint = inflectionPoint;
    }

    ExecutionMetrics.SLA_DATE_ITERATIONS.count(inflectionPointsToProject.size());
    return Optional.empty();
  }

  private static Map<ProcessPath, Map<Instant, Long>> backlogToWave(
      final Instant date,
      final PendingBacklog pending,
      final List<Wave> waves,
      final List<ProcessPath> processPaths,
      final Map<ProcessPath, Map<Instant, Instant>> deadlines
  ) {
    final var availableBacklog = pending.availableBacklogAt(date, processPaths, waves);

    return filterExpiredBacklogFromAvailableBacklog(date, availableBacklog, deadlines);
  }

  private static Map<ProcessPath, Map<Instant, Long>> filterExpiredBacklogFromAvailableBacklog(
      final Instant date,
      final Map<ProcessPath, Map<Instant, Long>> availableBacklog,
      final Map<ProcessPath, Map<Instant, Instant>> deadlines
  ) {
    return availableBacklog.entrySet()
        .stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().entrySet()
                    .stream()
                    .filter(cpt -> date.isBefore(deadlines.get(entry.getKey()).get(cpt.getKey())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            )
        );
  }

  private static Map<ProcessPath, List<Instant>> calculateSlaExpirationWithWaveSimulation(
      final Instant inflectionPoint,
      final SimulationContext context,
      final Map<ProcessPath, Map<Instant, Long>> backlogToWave
  ) {
    final var backlog = asBacklogs(inflectionPoint, context.getProjectedBacklogs());

    final var wave = Map.of(
        inflectionPoint, backlogToWave
    );

    final var ips = inflectionPointsGreaterOrEqual(inflectionPoint, context.getInflectionPoints());
    final var holder = buildContextHolder(backlog, context.getThroughput());
    final var slas = slasInWave(backlogToWave);
    final var projectedEndDates = projectSla(context.getGraph(), holder, wave, ips, context.getProcessPaths(), slas);

    return filterWavedSlasWithProjectedEndDateAfterDeadline(projectedEndDates, context.getDeadlines(), backlogToWave);
  }

  private static List<Instant> inflectionPointsGreaterOrEqual(final Instant target, final List<Instant> candidates) {
    return candidates.stream()
        .filter(ip -> !ip.isBefore(target))
        .sorted()
        .collect(Collectors.toList());
  }

  private static List<Instant> slasInWave(final Map<ProcessPath, Map<Instant, Long>> wave) {
    return wave.values()
        .stream()
        .map(Map::keySet)
        .flatMap(Set::stream)
        .distinct()
        .collect(Collectors.toList());
  }

  private static Optional<DateWaveSupplier> buildWaveFromExpiredSlas(
      final Instant waveDate,
      final Map<ProcessPath, Map<Instant, Long>> wavedBacklog,
      final Map<ProcessPath, List<Instant>> expiredSlas
  ) {
    final var expiredBacklog = calculateUnitsToWaveBySlaAndProcessPath(expiredSlas, wavedBacklog);

    final Map<ProcessPath, Long> lowerBounds = expiredBacklog.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().values().stream().reduce(0L, Long::sum)));

    final var configurations = expiredBacklog.keySet()
        .stream()
        .filter(pp -> lowerBounds.get(pp) > 0)
        .collect(Collectors.toMap(
            Function.identity(),
            pp -> new Wave.WaveConfiguration(lowerBounds.get(pp), Long.MAX_VALUE, expiredBacklog.get(pp))
        ));

    if (configurations.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(new DateWaveSupplier(waveDate, () -> new Wave(waveDate, SLA, configurations)));
  }

  private static Map<ProcessPath, Map<Instant, Long>> asBacklogs(
      final Instant inflectionPoint,
      final Map<Instant, List<CurrentBacklog>> currentBacklogs
  ) {
    return currentBacklogs.getOrDefault(inflectionPoint, emptyList())
        .stream()
        .collect(Collectors.groupingBy(
            CurrentBacklog::getProcessPath,
            Collectors.toMap(
                CurrentBacklog::getCpt,
                CurrentBacklog::getUnits
            )
        ));
  }

  private static Map<ProcessPath, Map<Instant, Instant>> calculateDeadlines(
      final Map<ProcessPath, Set<Instant>> slas,
      final Map<ProcessPath, Integer> minCycleTime
  ) {
    return slas.keySet()
        .stream()
        .collect(Collectors.toMap(
                Function.identity(),
                pp -> slas.get(pp)
                    .stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            sla -> sla.minus(minCycleTime.get(pp), ChronoUnit.MINUTES)
                        )
                    )
            )
        );
  }

  private static Map<ProcessPath, Map<Instant, Instant>> pessimisticCalculateDeadlines(
      final Map<ProcessPath, Set<Instant>> slas,
      final Map<ProcessPath, Integer> minCycleTime
  ) {
    return slas.keySet()
        .stream()
        .collect(Collectors.toMap(
                Function.identity(),
                pp -> slas.get(pp)
                    .stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            sla -> sla.minus(calculateMinutesToSubtract(sla, minCycleTime, pp), ChronoUnit.MINUTES)
                        )
                    )
            )
        );
  }

  private static int calculateMinutesToSubtract(
      final Instant sla,
      final Map<ProcessPath, Integer> minCycleTime,
      final ProcessPath processPath) {

    final int cycleTime = minCycleTime.get(processPath);

    if (ProcessPath.TOT_MULTI_BATCH.equals(processPath)) {
      return cycleTime;
    }
    final int sameDayMinutes = 0;
    final int notSameDayMinutes = 80;
    final int sameDaySla = 17;

    final ZonedDateTime zonedDateTime = sla.atZone(ZoneOffset.UTC);
    return zonedDateTime.getHour() == sameDaySla
        ? cycleTime + sameDayMinutes
        : cycleTime + notSameDayMinutes;

  }

  private static Map<ProcessPath, List<Instant>> filterWavedSlasWithProjectedEndDateAfterDeadline(
      final Map<ProcessPath, Map<Instant, Instant>> projections,
      final Map<ProcessPath, Map<Instant, Instant>> deadlines,
      final Map<ProcessPath, Map<Instant, Long>> backlogToWave
  ) {
    final var results = projections.keySet()
        .stream()
        .collect(Collectors.toMap(
                Function.identity(),
                pp -> filterWavedSlasWithProjectedEndDateAfterDeadline(
                    projections.get(pp),
                    deadlines.get(pp),
                    backlogToWave.getOrDefault(pp, emptyMap()).keySet()
                )
            )
        );

    return results.entrySet()
        .stream()
        .filter(entry -> !entry.getValue().isEmpty())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static List<Instant> filterWavedSlasWithProjectedEndDateAfterDeadline(
      final Map<Instant, Instant> projections,
      final Map<Instant, Instant> deadlines,
      final Set<Instant> wavedSlas
  ) {
    final var maxExpiredSla = wavedSlas.stream()
        .filter(sla -> Optional.ofNullable(projections.get(sla))
            .map(endDate -> endDate.isAfter(deadlines.get(sla)))
            .orElse(false)
        )
        .max(Comparator.naturalOrder());

    return maxExpiredSla.map(max ->
        deadlines.keySet()
            .stream()
            .filter(sla -> sla.isBefore(max) || sla.equals(max))
            .filter(wavedSlas::contains)
            .collect(Collectors.toList())
    ).orElse(emptyList());
  }

  private static Map<ProcessPath, Map<Instant, Long>> calculateUnitsToWaveBySlaAndProcessPath(
      final Map<ProcessPath, List<Instant>> expiredSlas,
      final Map<ProcessPath, Map<Instant, Long>> wave
  ) {
    return expiredSlas.keySet()
        .stream()
        .collect(Collectors.toMap(
            Function.identity(),
            pp -> expiredSlas.get(pp)
                .stream()
                .collect(Collectors.toMap(
                    Function.identity(),
                    sla -> wave.get(pp).getOrDefault(sla, 0L)
                ))
        ));
  }

  @Value
  public static class CurrentBacklog {

    ProcessPath processPath;

    Instant cpt;

    Long units;
  }

  /**
   * Contains all the static data.
   */
  @Value
  public static class SimulationContext {
    Map<ProcessPath, Map<Instant, Instant>> deadlines;

    List<ProcessPath> processPaths;

    Processor graph;

    List<Instant> inflectionPoints;

    Map<Instant, List<CurrentBacklog>> projectedBacklogs;

    Map<ProcessPath, Map<Instant, Integer>> throughput;

    PendingBacklog pendingBacklog;
  }

}
