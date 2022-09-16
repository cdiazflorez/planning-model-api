package com.mercadolibre.planning.model.api.projection;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.flow.projection.tools.services.entities.context.Backlog;
import com.mercadolibre.flow.projection.tools.services.entities.context.UpstreamByInflectionPoints;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDate;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.helpers.OrderedBacklogByDateRatioSplitter;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.projection.dto.ProjectionRequest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

class ProjectionUtils {
  private static final Duration HALF_AN_HOUR = Duration.ofMinutes(30);

  private static final Duration HOUR = Duration.ofHours(1);

  private static final String CONSOLIDATION_PROCESS_GROUP = "consolidation_group";

  private static final OrderedBacklogByDateRatioSplitter.Distribution<String> DEFAULT_PACKING_DISTRIBUTION =
      new OrderedBacklogByDateRatioSplitter.Distribution<>(Map.of(PACKING.getName(), 0.5, CONSOLIDATION_PROCESS_GROUP, 0.5));

  static List<Instant> generateInflectionPoints(final Instant dateFrom, final Instant dateTo) {
    final Instant dateFromTruncate = dateFrom.truncatedTo(ChronoUnit.HOURS);

    final List<Instant> inflectionPoints = new ArrayList<>();
    inflectionPoints.add(dateFrom);

    Instant date = dateFromTruncate.plus(HALF_AN_HOUR).isBefore(dateFrom)
        ? dateFromTruncate.plus(HOUR)
        : dateFromTruncate.plus(HALF_AN_HOUR);

    while (date.isBefore(dateTo) || date.equals(dateTo)) {
      inflectionPoints.add(date);
      date = date.plus(HALF_AN_HOUR);
    }

    return inflectionPoints;
  }

  static UpstreamByInflectionPoints mapForecastToUpstreamBacklog(final List<ProjectionRequest.PlanningDistribution> forecastSales) {

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

    return new UpstreamByInflectionPoints(upstreamBacklog);
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
