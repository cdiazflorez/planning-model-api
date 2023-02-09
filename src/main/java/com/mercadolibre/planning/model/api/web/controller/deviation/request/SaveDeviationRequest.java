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
  String warehouseId;

  @NotNull
  ZonedDateTime dateFrom;

  @NotNull
  ZonedDateTime dateTo;

  @NotNull
  double value;

  @NotNull
  Long userId;

  List<Path> paths;

  public SaveDeviationInput toDeviationInput(final Workflow workflow, final DeviationType deviationType) {
    return SaveDeviationInput
        .builder()
        .warehouseId(warehouseId)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .value(value)
        .userId(userId)
        .paths(paths)
        .workflow(workflow)
        .deviationType(deviationType)
        .build();
  }
}
