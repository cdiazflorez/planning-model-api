package com.mercadolibre.planning.model.api.projection.waverless;

import static com.mercadolibre.planning.model.api.util.DateUtils.generateInflectionPoints;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.BacklogProjection;
import com.mercadolibre.planning.model.api.projection.ProcessPathConfiguration;
import com.mercadolibre.planning.model.api.projection.UnitsByProcessPathAndProcess;
import com.mercadolibre.planning.model.api.projection.waverless.PendingBacklog.AvailableBacklog;
import com.mercadolibre.planning.model.api.projection.waverless.idleness.DateWaveSupplier;
import com.mercadolibre.planning.model.api.projection.waverless.idleness.NextIdlenessWaveProjector;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Value;

public final class WavesCalculator {

  private static final int MAX_WAVES_TO_PROJECT = 150;

  private static final int INFLECTION_WINDOW_SIZE_IN_MINUTES = 5;

  private WavesCalculator() {
  }

  @Trace
  public static TriggerProjection waves(
      final Instant executionDate,
      final List<ProcessPathConfiguration> configurations,
      final List<UnitsByProcessPathAndProcess> backlogs,
      final List<ForecastedUnitsByProcessPath> forecast,
      final Map<ProcessPath, Map<ProcessName, Map<Instant, Integer>>> throughput,
      final Map<ProcessPath, List<PrecalculatedWave>> precalculatedWaves,
      final String logisticCenterId,
      final WaveSizeConfig waveSizeConfig
  ) {
    final List<Instant> inflectionPoints = calculateInflectionPoints(executionDate, throughput);

    final PendingBacklog pendingBacklog = asPendingBacklog(executionDate, backlogs, forecast);

    final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentBacklog = asCurrentBacklogs(backlogs);

    final Map<ProcessPath, Integer> minCycleTimesByPP = configurations.stream()
        .collect(toMap(ProcessPathConfiguration::getProcessPath, ProcessPathConfiguration::getMinCycleTime));

    final List<Wave> waves = new ArrayList<>();
    boolean nextWaveHasBeenProjected = true;
    while (waves.size() < MAX_WAVES_TO_PROJECT && nextWaveHasBeenProjected) {
      final Optional<DateWaveSupplier> bySla = NextSlaWaveProjector.calculateNextWave(
          inflectionPoints,
          waves,
          pendingBacklog,
          currentBacklog.getOrDefault(ProcessName.PICKING, emptyMap()),
          throughput,
          minCycleTimesByPP,
          logisticCenterId
      );

      final Optional<DateWaveSupplier> byIdleness = NextIdlenessWaveProjector.calculateNextWave(
          inflectionPoints,
          pendingBacklog,
          currentBacklog,
          throughput,
          precalculatedWaves,
          waves,
          waveSizeConfig
      );

      final var wave = bySla.map(
              sla -> byIdleness.map(
                  idl -> idl.getExecutionDate().isBefore(sla.getExecutionDate()) ? byIdleness : bySla
              ).orElse(bySla)
          )
          .orElse(byIdleness)
          .map(DateWaveSupplier::getWave)
          .map(Supplier::get);

      wave.ifPresent(waves::add);
      nextWaveHasBeenProjected = wave.isPresent();
    }

    final var projectedBacklogs = BacklogProjection.project(inflectionPoints, waves, currentBacklog, throughput.get(ProcessPath.GLOBAL));

    return new TriggerProjection(waves, projectedBacklogs);
  }

  private static List<Instant> calculateInflectionPoints(
      final Instant executionDate,
      final Map<ProcessPath, Map<ProcessName, Map<Instant, Integer>>> throughput
  ) {
    final Instant projectionDateTo = throughput.values()
        .stream()
        .map(Map::values)
        .flatMap(Collection::stream)
        .map(Map::keySet)
        .flatMap(Set::stream)
        .max(Comparator.naturalOrder())
        .orElseThrow();

    return generateInflectionPoints(executionDate, projectionDateTo, INFLECTION_WINDOW_SIZE_IN_MINUTES);
  }

  private static PendingBacklog asPendingBacklog(
      final Instant executionDate,
      final List<UnitsByProcessPathAndProcess> backlogs,
      final List<ForecastedUnitsByProcessPath> forecast
  ) {

    final var currentBacklogs = backlogs.stream()
        .filter(backlog -> backlog.getProcessName() == ProcessName.WAVING)
        .collect(groupingBy(
            UnitsByProcessPathAndProcess::getProcessPath,
            Collectors.mapping(
                backlog -> new AvailableBacklog(executionDate, backlog.getDateOut(), (double) backlog.getUnits()),
                Collectors.toList()
            )
        ));

    final var forecastedBacklog = forecast.stream()
        .collect(groupingBy(
            ForecastedUnitsByProcessPath::getProcessPath,
            Collectors.mapping(
                backlog -> new AvailableBacklog(backlog.getDateIn(), backlog.getDateOut(), (double) backlog.getTotal()),
                Collectors.toList()
            )
        ));

    return new PendingBacklog(currentBacklogs, forecastedBacklog);
  }

  private static Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> asCurrentBacklogs(
      final List<UnitsByProcessPathAndProcess> backlogs
  ) {
    return backlogs.stream()
        .filter(backlog -> backlog.getProcessName() != ProcessName.WAVING)
        .collect(groupingBy(
                UnitsByProcessPathAndProcess::getProcessName,
                groupingBy(
                    UnitsByProcessPathAndProcess::getProcessPath,
                    toMap(
                        UnitsByProcessPathAndProcess::getDateOut,
                        backlog -> (long) backlog.getUnits(),
                        Long::sum
                    )
                )
            )
        );
  }

  @Value
  public static class TriggerProjection {
    List<Wave> waves;

    Map<ProcessName, Map<Instant, Long>> projectedBacklogs;

  }

}
