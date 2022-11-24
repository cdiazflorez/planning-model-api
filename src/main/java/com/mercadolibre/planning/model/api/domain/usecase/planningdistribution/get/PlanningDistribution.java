package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

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
public class PlanningDistribution {

  private static final double ADJUSTMENT_BASE = 1.0;

  long forecastId;
  Instant dateIn;
  Instant dateOut;
  ProcessPath processPath;
  double quantity;

  PlanningDistribution newWithAdjustment(final double value) {
    return new PlanningDistribution(
        forecastId,
        dateIn,
        dateOut,
        processPath,
        quantity * (ADJUSTMENT_BASE + value)
    );
  }
}
