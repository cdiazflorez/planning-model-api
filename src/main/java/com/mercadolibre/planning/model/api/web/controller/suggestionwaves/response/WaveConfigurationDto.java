package com.mercadolibre.planning.model.api.web.controller.suggestionwaves.response;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.util.NavigableSet;
import lombok.Value;

@Value
public class WaveConfigurationDto {
  ProcessPath processPath;

  int lowerBound;

  int upperBound;

  NavigableSet<Instant> slaDates;

}
