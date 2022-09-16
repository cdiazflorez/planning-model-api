package com.mercadolibre.planning.model.api.domain.usecase.forecast.get;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.Instant;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class GetForecastInput {
  String warehouseId;

  Workflow workflow;

  ZonedDateTime dateFrom;

  ZonedDateTime dateTo;

  Instant viewDate;

  public GetForecastInput(final String warehouseId, final Workflow workflow, final ZonedDateTime dateFrom, final ZonedDateTime dateTo) {
    this.warehouseId = warehouseId;
    this.workflow = workflow;
    this.dateFrom = dateFrom;
    this.dateTo = dateTo;
    this.viewDate = null;
  }
}
