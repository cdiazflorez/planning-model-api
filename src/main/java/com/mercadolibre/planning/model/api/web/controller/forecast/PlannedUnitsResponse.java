package com.mercadolibre.planning.model.api.web.controller.forecast;

import java.util.Map;
import lombok.Value;

@Value
public class PlannedUnitsResponse {
  Map<String, String> group;
  float total;
}
