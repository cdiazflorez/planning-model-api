package com.mercadolibre.planning.model.api.web.controller.suggestionwaves.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.ProcessPathConfiguration;
import com.mercadolibre.planning.model.api.projection.UnitsByProcessPathAndProcess;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Value;

@Value
public class SuggestionsWavesRequestDto {

    Instant viewDate;

    List<ProcessPathConfiguration> processPathConfigurations;

    List<UnitsByProcessPathAndProcess> backlogs;

    Map<ProcessPath, Map<Instant, Float>> throughputRatios;

    Map<ProcessName, Map<Instant, Integer>> backlogLimits;
}
