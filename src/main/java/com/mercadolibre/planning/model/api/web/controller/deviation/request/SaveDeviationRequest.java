package com.mercadolibre.planning.model.api.web.controller.deviation.request;

import com.mercadolibre.planning.model.api.domain.entity.DeviationType;
import com.mercadolibre.planning.model.api.domain.entity.Path;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save.SaveDeviationInput;
import java.time.ZonedDateTime;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class SaveDeviationRequest {
  @NotNull
  String logisticCenterId;

  @NotNull
  Workflow workflow;

  @NotNull
  List<Path> affectedShipmentTypes;

  @NotNull
  DeviationType type;

  @NotNull
  ZonedDateTime dateFrom;

  @NotNull
  ZonedDateTime dateTo;

  @NotNull
  double value;

  @NotNull
  long userId;

  public SaveDeviationInput toDeviationInput() {
    return SaveDeviationInput
        .builder()
        .warehouseId(logisticCenterId)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .value(value)
        .userId(userId)
        .workflow(workflow)
        .deviationType(type)
        .paths(affectedShipmentTypes)
        .build();
  }

  public SaveDeviationInput toDeviationInput(final Workflow workflow, final String logisticCenterId) {
    return SaveDeviationInput
        .builder()
        .warehouseId(logisticCenterId)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .value(value)
        .userId(userId)
        .workflow(workflow)
        .deviationType(type)
        .paths(affectedShipmentTypes)
        .build();
  }
}
