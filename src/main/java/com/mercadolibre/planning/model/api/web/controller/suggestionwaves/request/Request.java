package com.mercadolibre.planning.model.api.web.controller.suggestionwaves.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.ProcessPathConfiguration;
import com.mercadolibre.planning.model.api.projection.UnitsByProcessPathAndProcess;
import com.mercadolibre.planning.model.api.projection.waverless.ForecastedUnitsByProcessPath;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Value;

@Value
public class Request {
  Instant viewDate;

  List<ProcessPathConfiguration> processPathConfigurations;

  List<UnitsByProcessPathAndProcess> backlogs;

  List<ForecastedUnitsByProcessPath> forecast;

  Map<ProcessPath, Map<Instant, Float>> throughput;

  Map<ProcessName, Map<Instant, Integer>> backlogLimits;
}
