package com.mercadolibre.planning.model.api.web.controller.planningdistribution;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class GetPlanningDistributionResponse {

  ZonedDateTime dateIn;

  ZonedDateTime dateOut;

  MetricUnit metricUnit;

  long total;

  boolean isDeferred;

}
