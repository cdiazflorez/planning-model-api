package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class PlanDistribution {

  private static final double ADJUSTMENT_BASE = 1.0;

  long forecastId;
  Instant dateIn;
  Instant dateOut;
  ProcessPath processPath;
  MetricUnit metricUnit;
  double quantity;

  PlanDistribution newWithAdjustment(final double value) {
    return new PlanDistribution(
        forecastId,
        dateIn,
        dateOut,
        processPath,
        metricUnit,
        quantity * (ADJUSTMENT_BASE + value)
    );
  }
}
