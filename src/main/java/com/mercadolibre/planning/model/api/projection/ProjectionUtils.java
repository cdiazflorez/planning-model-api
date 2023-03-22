package com.mercadolibre.planning.model.api.projection;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.flow.projection.tools.services.entities.context.Backlog;
import com.mercadolibre.flow.projection.tools.services.entities.context.PiecewiseUpstream;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDate;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.helpers.OrderedBacklogByDateRatioSplitter;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.projection.dto.ProjectionRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

class ProjectionUtils {

  private static final String CONSOLIDATION_PROCESS_GROUP = "consolidation_group";

  private static final OrderedBacklogByDateRatioSplitter.Distribution<String> DEFAULT_PACKING_DISTRIBUTION =
      new OrderedBacklogByDateRatioSplitter.Distribution<>(Map.of(PACKING.getName(), 0.5, CONSOLIDATION_PROCESS_GROUP, 0.5));

  private ProjectionUtils() {

  }

  static PiecewiseUpstream mapForecastToUpstreamBacklog(final List<ProjectionRequest.PlanningDistribution> forecastSales) {

    final Map<Instant, Backlog> upstreamBacklog = forecastSales.stream()
        .filter(entry -> entry.getTotal() > 0)
        .collect(
            groupingBy(
                pu -> pu.getDateIn().toInstant(),
                collectingAndThen(
                    toMap(
                        pd -> pd.getDateOut().toInstant(),
                        ProjectionRequest.PlanningDistribution::getTotal,
                        Long::sum
                    ),
                    map -> new OrderedBacklogByDate(
                        map.entrySet()
                            .stream()
                            .collect(toMap(
                                Map.Entry::getKey,
                                entry -> new OrderedBacklogByDate.Quantity(entry.getValue())
                            ))
                    )
                )
            )
        );

    return new PiecewiseUpstream(upstreamBacklog);
  }

  static Map<Instant, OrderedBacklogByDateRatioSplitter.Distribution<String>> ratiosAsDistributions(
      final Map<Instant, ProjectionRequest.PackingRatio> ratioByHour,
      final List<Instant> inflectionPoints) {
    final var baseRatios = ratioByHour.entrySet()
        .stream()
        .collect(toMap(
            Map.Entry::getKey,
            entry -> new OrderedBacklogByDateRatioSplitter.Distribution<>(
                Map.of(
                    PACKING.getName(), entry.getValue().getPackingToteRatio(),
                    CONSOLIDATION_PROCESS_GROUP, entry.getValue().getPackingWallRatio()
                )
            )
        ));

    return inflectionPoints.stream()
        .collect(toMap(
            Function.identity(),
            ip -> baseRatios.getOrDefault(ip.truncatedTo(ChronoUnit.HOURS), DEFAULT_PACKING_DISTRIBUTION)
        ));
  }

  static Set<Instant> obtainDateOutsFrom(final Map<ProcessName, Map<Instant, Integer>> backlogBySlaAndProcess) {
    return backlogBySlaAndProcess.values().stream()
        .flatMap(backlogBySla -> backlogBySla.keySet().stream())
        .collect(Collectors.toSet());
  }
}
