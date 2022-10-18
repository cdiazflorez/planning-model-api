package com.mercadolibre.planning.model.api.web.controller.forecast.request;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import java.time.ZonedDateTime;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class PolyvalentProductivityRequest {

  private static final Integer ONE_HUNDRED_PERCENT = 100;

  @NotNull
  ProcessName processName;

  @NotNull
  MetricUnit productivityMetricUnit;

  long productivity;

  int abilityLevel;

  public HeadcountProductivity toHeadcountProductivity(
      final Forecast forecast,
      final ProcessPath processPath,
      final long regularProductivity,
      final ZonedDateTime date
  ) {
    return HeadcountProductivity.builder()
        .abilityLevel(abilityLevel)
        .processPath(processPath)
        .processName(processName)
        .productivityMetricUnit(UNITS_PER_HOUR)
        .date(date)
        .productivity((regularProductivity * productivity) / ONE_HUNDRED_PERCENT)
        .forecast(forecast)
        .build();
  }
}
