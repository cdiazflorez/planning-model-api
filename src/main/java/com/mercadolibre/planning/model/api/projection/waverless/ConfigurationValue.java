package com.mercadolibre.planning.model.api.projection.waverless;

import java.util.Map;
import lombok.Value;

@Value
public class ConfigurationValue {

  int value;
  Map<String, String> tags;

}
