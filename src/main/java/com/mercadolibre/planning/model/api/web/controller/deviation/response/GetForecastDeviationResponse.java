package com.mercadolibre.planning.model.api.web.controller.deviation.response;

import com.mercadolibre.planning.model.api.domain.entity.DeviationType;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.Path;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GetForecastDeviationResponse {

  Workflow workflow;

  ZonedDateTime dateFrom;

  ZonedDateTime dateTo;

  double value;

  MetricUnit metricUnit;

  DeviationType type;

  Path path;

}
