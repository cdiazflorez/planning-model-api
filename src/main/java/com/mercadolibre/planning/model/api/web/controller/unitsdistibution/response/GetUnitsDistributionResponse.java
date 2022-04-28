package com.mercadolibre.planning.model.api.web.controller.unitsdistibution.response;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import java.time.ZonedDateTime;
import lombok.Value;

/** Class that returns the units-distribution parameters. */
@Value
public class GetUnitsDistributionResponse {

  String logisticCenterId;

  ZonedDateTime date;

  ProcessName processName;

  String area;

  Double quantity;

  MetricUnit quantityMetricUnit;
}
