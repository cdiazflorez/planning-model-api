package com.mercadolibre.planning.model.api.projection.waverless;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;

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
      final List<AvailableBacklog> forecast
  ) {
    return Stream.concat(
        readyToWave.stream(),
        mapForecast(inflectionPoint, forecast)
    ).collect(
        groupingBy(
            AvailableBacklog::getDateOut,
            mapping(AvailableBacklog::getQuantity, collectingAndThen(reducing(0D, Double::sum), Double::longValue))
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

  public Map<ProcessPath, Map<Instant, Long>> availableBacklogAt(final Instant inflectionPoint, final List<ProcessPath> processPaths) {
    return processPaths.stream()
        .collect(Collectors.toMap(
                Function.identity(),
                pp -> availableBacklogFromProcessPathAt(
                    inflectionPoint,
                    readyToWave.getOrDefault(pp, emptyList()),
                    forecast.getOrDefault(pp, emptyList())
                )
            )
        );
  }

  public Map<ProcessPath, Set<Instant>> calculateSlasByProcessPath() {
    return readyToWave.keySet()
        .stream()
        .collect(Collectors.toMap(
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

}
