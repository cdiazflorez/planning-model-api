package com.mercadolibre.planning.model.api.projection.waverless;

import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.flow.projection.tools.services.entities.context.Upstream;
import com.mercadolibre.flow.projection.tools.services.entities.context.UpstreamAtInflectionPoint;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.backlogmanager.OrderedBacklogByProcessPath;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ProjectionUtils {

  private ProjectionUtils() {
  }

  public static Upstream asUpstream(final List<Wave> waves) {
    final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> wavesByDate = waves.stream()
        .collect(
            groupingBy(
                Wave::getDate,
                flatMapping(
                    wave -> wave.getConfiguration()
                        .entrySet()
                        .stream(),
                    groupingBy(
                        Map.Entry::getKey,
                        flatMapping(
                            entry -> entry.getValue()
                                .getWavedUnitsByCpt()
                                .entrySet()
                                .stream(),
                            toMap(Map.Entry::getKey, Map.Entry::getValue, Long::sum)
                        )
                    )
                )
            )
        );

    return asUpstream(wavesByDate);
  }

  public static Upstream asUpstream(final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> waves) {
    return new UpstreamAtInflectionPoint(
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
