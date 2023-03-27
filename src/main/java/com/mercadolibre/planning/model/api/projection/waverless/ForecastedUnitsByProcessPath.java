package com.mercadolibre.planning.model.api.projection.waverless;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import lombok.Value;

@Value
public class ForecastedUnitsByProcessPath {
  ProcessPath processPath;

  Instant dateIn;

  Instant dateOut;

  float total;
}
