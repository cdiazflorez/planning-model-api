package com.mercadolibre.planning.model.api.projection.waverless;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.summingLong;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * PendingBacklog encapsulates both the backlog ready to wave and the forecasted backlog and exposes a method to calculate the
 * backlog that will be in this state at some date.
 */
@RequiredArgsConstructor
public class PendingBacklog {

  private static final long NO_BACKLOG = 0L;

  private final Map<ProcessPath, List<AvailableBacklog>> readyToWave;

  private final Map<ProcessPath, List<AvailableBacklog>> forecast;

  private static Set<Instant> slasFromBacklogs(final List<AvailableBacklog> readyToWave, final List<AvailableBacklog> forecast) {
    return Stream.concat(readyToWave.stream(), forecast.stream())
        .map(AvailableBacklog::getDateOut)
        .collect(Collectors.toSet());
  }

  private static Map<Instant, Long> availableBacklogFromProcessPathAt(
      final Instant inflectionPoint,
      final List<AvailableBacklog> readyToWave,
      final List<AvailableBacklog> forecast,
      final Map<Instant, Long> wavesBacklogBySla
  ) {
    final var backlogToWave = Stream.concat(
        readyToWave.stream(),
        mapForecast(inflectionPoint, forecast)
    ).collect(
        groupingBy(
            AvailableBacklog::getDateOut,
            mapping(AvailableBacklog::getQuantity, collectingAndThen(reducing(0D, Double::sum), Double::longValue))
        )
    );

    return backlogToWave.entrySet()
        .stream()
        .filter(entry -> entry.getValue() > wavesBacklogBySla.getOrDefault(entry.getKey(), NO_BACKLOG))
        .collect(
            toMap(
                Map.Entry::getKey,
                entry -> entry.getValue() - wavesBacklogBySla.getOrDefault(entry.getKey(), NO_BACKLOG)
            )
        );


  }

  private static Stream<AvailableBacklog> mapForecast(final Instant inflectionPoint, final List<AvailableBacklog> forecast) {
    final var inflectionHour = inflectionPoint.truncatedTo(HOURS);
    final var minutes = ZonedDateTime.ofInstant(inflectionPoint, ZoneOffset.UTC).getMinute();

    final var multiplier = minutes / 60D;

    return Stream.concat(
        forecast.stream()
            .filter(backlog -> backlog.getDateIn().isBefore(inflectionHour)),
        forecast.stream()
            .filter(backlog -> backlog.getDateIn().equals(inflectionHour))
            .map(backlog -> new AvailableBacklog(
                backlog.getDateIn(),
                backlog.getDateOut(),
                backlog.getQuantity() * multiplier
            ))
    );
  }

  private static Map<ProcessPath, Map<Instant, Long>> reduceWavesByProcessPath(final List<Wave> waves) {
    final var pendingProcessPathBacklogStream = waves.stream().map(Wave::getConfiguration)
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .flatMap(entry -> entry.getValue()
            .getWavedUnitsByCpt().entrySet().stream().map(
                wavedUnitsByCpt ->
                    new PendingProcessPathBacklog(
                        entry.getKey(),
                        wavedUnitsByCpt.getKey(),
                        wavedUnitsByCpt.getValue()
                    )
            )
        );

    return pendingProcessPathBacklogStream
        .collect(groupingBy(PendingProcessPathBacklog::getProcessPath,
            groupingBy(PendingProcessPathBacklog::getSla, summingLong(PendingProcessPathBacklog::getQuantity)))
        );
  }

  public Map<ProcessPath, Map<Instant, Long>> availableBacklogAt(
      final Instant inflectionPoint, final List<ProcessPath> processPaths, final List<Wave> waves) {

    final var wavesByProcessPath = reduceWavesByProcessPath(waves);

    return processPaths.stream()
        .collect(toMap(
                Function.identity(),
                pp -> availableBacklogFromProcessPathAt(
                    inflectionPoint,
                    readyToWave.getOrDefault(pp, emptyList()),
                    forecast.getOrDefault(pp, emptyList()),
                    wavesByProcessPath.getOrDefault(pp, emptyMap())
                )
            )
        );
  }

  public Map<ProcessPath, Set<Instant>> calculateSlasByProcessPath() {
    return readyToWave.keySet()
        .stream()
        .collect(toMap(
            Function.identity(),
            pp -> slasFromBacklogs(
                readyToWave.getOrDefault(pp, emptyList()),
                forecast.getOrDefault(pp, emptyList())
            )
        ));
  }

  @Value
  public static class AvailableBacklog {
    Instant dateIn;

    Instant dateOut;

    Double quantity;
  }

  @Value
  private static class PendingProcessPathBacklog {
    ProcessPath processPath;

    Instant sla;

    Long quantity;
  }

}
