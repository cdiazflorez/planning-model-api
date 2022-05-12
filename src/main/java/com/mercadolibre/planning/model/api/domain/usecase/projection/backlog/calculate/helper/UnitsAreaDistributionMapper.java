package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.helper;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.BacklogByArea;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.BacklogBySla;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.QuantityAtArea;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maps from BacklogBySla to BacklogByArea based on share distribution.
 */
public class UnitsAreaDistributionMapper implements BacklogMapper<BacklogBySla, BacklogByArea> {

  private static final String GLOBAL_AREA = "NA";

  private static final Map<String, Double> NO_AREAS_DISTRIBUTIONS = Map.of(GLOBAL_AREA, 1.0);

  private final Map<ProcessName, Map<Instant, Map<String, Double>>> areaDistributionBySlaByProcess;

  private final Map<ProcessName, List<String>> areasByProcess;

  public UnitsAreaDistributionMapper(final Map<ProcessName, Map<Instant, Map<String, Double>>> areaDistributionBySlaByProcess) {
    this.areaDistributionBySlaByProcess = areaDistributionBySlaByProcess;
    this.areasByProcess = areaDistributionBySlaByProcess.entrySet()
        .stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue()
                .values()
                .stream()
                .flatMap(areaDist -> areaDist.keySet().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList())
        ));
  }


  @Override
  public BacklogByArea map(final ProcessName process, final BacklogBySla backlogByCpt) {
    final var backlogs = backlogByCpt.getDistributions();

    final Map<String, Double> quantityByArea = backlogs.stream()
        .flatMap(backlog ->
            this.distributeQuantityAlongAreasAccordingToDistribution(backlog.getQuantity(), getAreaDistribution(process, backlog.getDate()))
        )
        .collect(Collectors.toMap(
            QuantityAtArea::getArea,
            QuantityAtArea::getQuantity,
            Double::sum
        ));

    final var areas = areasByProcess.getOrDefault(process, Collections.emptyList());

    final var isNoAreaPresent = quantityByArea.containsKey(GLOBAL_AREA);
    final var noAreaQuantity = new QuantityAtArea(GLOBAL_AREA, quantityByArea.getOrDefault(GLOBAL_AREA, 0.0));
    final Stream<QuantityAtArea> noAreaStream = isNoAreaPresent ? Stream.of(noAreaQuantity) : Stream.empty();

    return new BacklogByArea(
        Stream.concat(
            areas.stream()
                .map(area -> new QuantityAtArea(area, quantityByArea.getOrDefault(area, 0.0))),
            noAreaStream
        ).collect(Collectors.toList())
    );
  }

  private Map<String, Double> getAreaDistribution(final ProcessName process, final Instant date) {
    return Optional.ofNullable(areaDistributionBySlaByProcess.get(process))
        .map(values -> values.get(date))
        .orElse(NO_AREAS_DISTRIBUTIONS);
  }

  private Stream<QuantityAtArea> distributeQuantityAlongAreasAccordingToDistribution(final int quantity,
                                                                                     final Map<String, Double> areaDistribution) {
    return areaDistribution.entrySet()
        .stream()
        .map(entry ->
            new QuantityAtArea(entry.getKey(), entry.getValue() * quantity)
        );
  }

}
