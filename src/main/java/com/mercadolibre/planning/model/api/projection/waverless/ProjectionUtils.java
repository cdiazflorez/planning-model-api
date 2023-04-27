package com.mercadolibre.planning.model.api.projection.waverless;

import static java.util.stream.Collectors.toMap;

import com.mercadolibre.flow.projection.tools.services.entities.context.PiecewiseUpstream;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ProjectionUtils {

  private static final long INFLECTION_POINT_WINDOW_SIZE = 5L;

  private static final Map<ProcessPath, Map<Instant, Long>> EMPTY_WAVE = Map.of(ProcessPath.TOT_MONO, Map.of(Instant.now(), 0L));

  private ProjectionUtils() {
  }

  public static PiecewiseUpstream toPiecewiseUpstream(final List<Wave> waves) {
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

    // TODO: replace this when updating lib upstream backlog to an interface
    final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> fixedWaves = new HashMap<>();
    wavesByDate.forEach((date, wave) -> fixedWaves.put(date.plus(INFLECTION_POINT_WINDOW_SIZE, ChronoUnit.MINUTES), EMPTY_WAVE));
    fixedWaves.putAll(wavesByDate);

    return asPiecewiseUpstream(fixedWaves);
  }

  public static PiecewiseUpstream asPiecewiseUpstream(final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> waves) {
    return new PiecewiseUpstream(
        waves.entrySet().stream()
            .collect(
                toMap(
                    Map.Entry::getKey,
                    entry -> OrderedBacklogByProcessPath.from(entry.getValue())
                )
            )
    );
  }
}
