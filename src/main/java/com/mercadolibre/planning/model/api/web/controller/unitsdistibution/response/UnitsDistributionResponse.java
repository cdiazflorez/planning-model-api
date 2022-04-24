package com.mercadolibre.planning.model.api.web.controller.unitsdistibution.response;

import lombok.Value;

/**
 * Class that returns the number of units-distribution
 * saved
 */
@Value
public class UnitsDistributionResponse {

  String response;

  int quantitySave;
}
