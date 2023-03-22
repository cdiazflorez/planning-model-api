package com.mercadolibre.planning.model.api.web.controller.suggestionwaves.response;

import com.mercadolibre.planning.model.api.domain.entity.TriggerName;
import java.time.Instant;
import java.util.List;
import lombok.Value;

@Value
public class WaveDto {
  Instant date;

  List<WaveConfigurationDto> waves;

  TriggerName reason;
}
