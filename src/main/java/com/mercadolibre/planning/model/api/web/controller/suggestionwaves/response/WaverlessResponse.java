package com.mercadolibre.planning.model.api.web.controller.suggestionwaves.response;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Value;

@Value
public class WaverlessResponse {
  String logisticCenterId;

  Instant executionDate;

  List<WaveDto> suggestions;

  Map<ProcessName, List<UnitsAtOperationHour>> projectedBacklogs;

  @Value
  public static class UnitsAtOperationHour {
    Instant date;

    long quantity;

  }
}
