package com.mercadolibre.planning.model.api.domain.entity.plan;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import java.util.Map;

public record StaffingPlan(
  double quantity,
  MetricUnit quantityMetricUnit,
  ProcessingType type,
  Map<String, String> grouper
) {
}
