package com.mercadolibre.planning.model.api.web.controller.ratios.response;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class PreloadPackingRatiosOutput {

  String logisticCenterId;

  boolean isRatioSaved;

}
