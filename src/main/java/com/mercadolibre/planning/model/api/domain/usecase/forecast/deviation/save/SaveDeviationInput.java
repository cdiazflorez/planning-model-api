package com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save;

import com.mercadolibre.planning.model.api.domain.entity.DeviationType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class SaveDeviationInput {

  String warehouseId;

  ZonedDateTime dateFrom;

  ZonedDateTime dateTo;

  double value;

  Long userId;

  Workflow workflow;

  DeviationType deviationType;

  public CurrentForecastDeviation toCurrentForecastDeviation() {
    return CurrentForecastDeviation
        .builder()
        .logisticCenterId(warehouseId)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .value(value)
        .isActive(true)
        .userId(userId)
        .workflow(workflow)
        .type(deviationType)
        .build();
  }
}
