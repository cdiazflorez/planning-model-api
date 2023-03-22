package com.mercadolibre.planning.model.api.web.controller.suggestionwaves.response;

import java.time.Instant;
import java.util.List;
import lombok.Value;

@Value
public class WaverlessResponse {
    String logisticCenterId;
    Instant executionDate;
    List<WaveDto> suggestions;
}
