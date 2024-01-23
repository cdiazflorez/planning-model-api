package com.mercadolibre.planning.model.api.projection.waverless;

import java.util.List;
import lombok.Value;

@Value
public class WaveSizeConfig {

  List<ConfigurationValue> tphMinutesForLowerLimit;

  List<ConfigurationValue> tphMinutesForUpperLimit;

  List<ConfigurationValue> tphMinutesForIdleness;

}
