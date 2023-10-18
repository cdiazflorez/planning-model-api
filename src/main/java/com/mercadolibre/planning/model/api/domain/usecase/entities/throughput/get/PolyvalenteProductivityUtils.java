package com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get;

import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.ProductivityOutput;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class PolyvalenteProductivityUtils {
  private PolyvalenteProductivityUtils() {
  }

  static PolyvalentProductivityRatio calculatePolyvalentProductivityRatioByProcessPath(
      final Map<ProcessPath, Map<ProcessName, Map<ZonedDateTime, Map<Source, ProductivityOutput>>>> regularProductivityMap,
      final Map<ProcessPath, Map<ProcessName, Map<ZonedDateTime, ProductivityOutput>>> polyvalentProductivityMap
  ) {
    if (regularProductivityMap == null || polyvalentProductivityMap == null) {
      return PolyvalentProductivityRatio.empty();
    }

    // TODO: this could be retrieved from the forecast metadata instead of recalculated
    final var ratios = regularProductivityMap.entrySet()
        .stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            processPathEntry -> calculatePolyvalentProductivityRatioByProcess(
                processPathEntry.getValue(),
                polyvalentProductivityMap.get(processPathEntry.getKey())
            )
        ));

    return new PolyvalentProductivityRatio(ratios);
  }

  private static Map<ProcessName, Map<ZonedDateTime, Double>> calculatePolyvalentProductivityRatioByProcess(
      final Map<ProcessName, Map<ZonedDateTime, Map<Source, ProductivityOutput>>> regularProductivityMap,
      final Map<ProcessName, Map<ZonedDateTime, ProductivityOutput>> polyvalentProductivityMap
  ) {
    return regularProductivityMap.entrySet()
        .stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            processEntry -> processEntry.getValue()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    dateEntry -> calculatePolyvalentProductivityRatio(
                        processEntry.getKey(),
                        dateEntry.getKey(),
                        dateEntry.getValue(),
                        polyvalentProductivityMap
                    ))
                ))
        );
  }

  private static double calculatePolyvalentProductivityRatio(
      final ProcessName processName,
      final ZonedDateTime date,
      final Map<Source, ProductivityOutput> regularProductivityMap,
      final Map<ProcessName, Map<ZonedDateTime, ProductivityOutput>> polyvalentProductivityMap
  ) {
    return regularProductivityMap
        .entrySet()
        .stream()
        .filter(entry -> entry.getKey() == FORECAST)
        .findFirst()
        .map(Map.Entry::getValue)
        .map(ProductivityOutput::getValue)
        .flatMap(regularProductivity -> Optional.ofNullable(polyvalentProductivityMap)
            .map(polyvalentProductivityByDate -> polyvalentProductivityByDate.get(processName))
            .map(polyvalentProductivityByDate -> polyvalentProductivityByDate.get(date))
            .map(ProductivityOutput::getValue)
            .map(polyvalentProductivity -> (polyvalentProductivity == 0
                ? regularProductivity
                : polyvalentProductivity) / regularProductivity)
        ).orElse(1D);
  }
}
