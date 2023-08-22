package com.mercadolibre.planning.model.api.web.controller.deviation.request;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class SaveDeviationsContentRequest {
  @NotNull
  String logisticCenterId;

  @NotNull
  List<SaveDeviationRequest> deviations;
}
