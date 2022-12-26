package com.mercadolibre.planning.model.api.web.controller.suggestionwaves.response;

import com.mercadolibre.planning.model.api.projection.Suggestion;
import java.time.Instant;
import java.util.List;
import lombok.Value;

@Value
public class SuggestionWavesDto {
    String logisticCenterId;
    Instant executionDate;
    List<Suggestion> suggestions;
}
