package com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.disable;

import com.mercadolibre.planning.model.api.domain.entity.DeviationType;
import com.mercadolibre.planning.model.api.domain.entity.Path;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.util.List;
import lombok.Value;

@Value
public class DisableForecastDeviationInput {

  private String warehouseId;

  private Workflow workflow;

  private DeviationType deviationType;

  private List<Path> affectedShipmentTypes;
}
