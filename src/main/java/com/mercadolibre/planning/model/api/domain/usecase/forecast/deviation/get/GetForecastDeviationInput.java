package com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.get;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.ZonedDateTime;
import lombok.Value;

@Value
public class GetForecastDeviationInput {

  String warehouseId;

  Workflow workflow;

  ZonedDateTime date;

}
