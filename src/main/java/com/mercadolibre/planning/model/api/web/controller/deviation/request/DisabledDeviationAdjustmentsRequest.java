package com.mercadolibre.planning.model.api.web.controller.deviation.request;

import com.mercadolibre.planning.model.api.domain.entity.DeviationType;
import com.mercadolibre.planning.model.api.domain.entity.Path;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.disable.DisableForecastDeviationInput;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class DisabledDeviationAdjustmentsRequest {
  @NotNull
  Workflow workflow;

  @NotNull
  DeviationType type;

  @NotNull
  List<Path> affectedShipmentTypes;

  public DisableForecastDeviationInput toDisableDeviationInput(final String logisticCenterId) {
    return new DisableForecastDeviationInput(logisticCenterId, workflow, type, affectedShipmentTypes);
  }

}
