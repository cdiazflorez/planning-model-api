package com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Value;

/** Class that contains the parameters of units-distribution. */
@AllArgsConstructor
@Value
public class UnitsInput {

  String logisticCenterId;

  ZonedDateTime date;

  ProcessName processName;

  String area;

  Double quantity;

  String quantityMetricUnit;

  Workflow workflow;
}
