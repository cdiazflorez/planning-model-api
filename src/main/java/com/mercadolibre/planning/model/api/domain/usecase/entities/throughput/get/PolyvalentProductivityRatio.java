package com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PolyvalentProductivityRatio {

  private static final PolyvalentProductivityRatio EMPTY = new PolyvalentProductivityRatio(Collections.emptyMap());

  private final Map<ProcessPath, Map<ProcessName, Map<ZonedDateTime, Double>>> ratiosByProcessPathProcessNameAndDate;

  static PolyvalentProductivityRatio empty() {
    return EMPTY;
  }

  Optional<Double> getForProcessPathProcessNameAndDate(final ProcessPath processPath, final ProcessName processName, final ZonedDateTime date) {
    return Optional.of(ratiosByProcessPathProcessNameAndDate)
        .map(ratios -> ratios.get(processPath))
        .map(ratios -> ratios.get(processName))
        .map(ratios -> ratios.get(date));
  }

}
