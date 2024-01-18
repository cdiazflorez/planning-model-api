package com.mercadolibre.planning.model.api.web.controller.forecast.dto;


import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public record StaffingPlanDto(
    ProcessingType type,
    Map<String, String> tags,
    MetricUnit quantityMetricUnit,
    double quantity
) {
  private static final String PROCESS_PATH_KEY = "process_path";
  private static final String PROCESS_KEY = "process";
  private static final String DATE_KEY = "date";
  private static final String POLYVALENCE_KEY = "polyvalence";

  public ProcessingDistribution toProcessingDists(final Forecast forecast) {
    final var tagsCopy = new HashMap<>(tags);
    final var date = tagsCopy.remove(DATE_KEY);
    return ProcessingDistribution.builder()
        //ToDo remove the line below when processing_distribution.process_path column will be removed.
        .processPath(tags.get(PROCESS_PATH_KEY) == null
            ? ProcessPath.GLOBAL
            : ProcessPath.of(tags.get(PROCESS_PATH_KEY)).orElse(ProcessPath.UNKNOWN))
        //ToDo remove the line below when processing_distribution.process_name column will be removed.
        .processName(ProcessName.of(tags.get(PROCESS_KEY)).orElse(ProcessName.GLOBAL))
        .quantityMetricUnit(quantityMetricUnit)
        .type(type)
        .date(ZonedDateTime.parse(date))
        .forecast(forecast)
        .tags(tagsCopy.toString())
        .quantity(quantity)
        .build();
  }

  /**
   * Deprecated method to map HeadcountProductivity, headcount_productivity table will disappear.
   * @param forecast get the forecast.
   * @return HeadcountProductivity.
   *
   * @deprecated use toProcessingDists instead.
   */
  @Deprecated
  public HeadcountProductivity toProductivity(Forecast forecast) {
    final var tagsCopy = new HashMap<>(tags);
    final var date = tagsCopy.remove(DATE_KEY);
    return HeadcountProductivity.builder()
        .processPath(tags.get(PROCESS_PATH_KEY) == null
            ? ProcessPath.GLOBAL
            : ProcessPath.of(tags.get(PROCESS_PATH_KEY)).orElse(ProcessPath.UNKNOWN))
        .processName(ProcessName.of(tags.get(PROCESS_KEY)).orElse(ProcessName.GLOBAL))
        .productivityMetricUnit(quantityMetricUnit)
        .abilityLevel(Integer.parseInt(tags.get(POLYVALENCE_KEY)))
        .date(ZonedDateTime.parse(date))
        .forecast(forecast)
        .productivity((long) quantity)
        .build();
  }
}
