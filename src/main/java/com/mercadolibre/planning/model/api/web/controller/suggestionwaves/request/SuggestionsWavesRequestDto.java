package com.mercadolibre.planning.model.api.web.controller.suggestionwaves.request;

import com.mercadolibre.planning.model.api.projection.ProcessPathConfiguration;
import com.mercadolibre.planning.model.api.projection.UnitsByProcessPathAndProcess;
import java.time.Instant;
import java.util.List;
import lombok.Value;

@Value
public class SuggestionsWavesRequestDto {
    Instant viewDate;
    List<ProcessPathConfiguration> processPathConfigurations;
    List<UnitsByProcessPathAndProcess> backlogs;
}
