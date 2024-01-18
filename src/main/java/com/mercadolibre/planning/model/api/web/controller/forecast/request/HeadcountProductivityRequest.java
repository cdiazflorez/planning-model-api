package com.mercadolibre.planning.model.api.web.controller.forecast.request;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class HeadcountProductivityRequest implements TagsBuilder {

  @NotNull
  ProcessPath processPath;

  @NotNull
  ProcessName processName;

  @NotNull
  MetricUnit productivityMetricUnit;

  int abilityLevel;

  @NotEmpty
  @Valid
  List<HeadcountProductivityDataRequest> data;

  public List<HeadcountProductivity> toHeadcountProductivities(
      final Forecast forecast,
      final List<PolyvalentProductivityRequest> polyvalentProductivities
  ) {
    return Stream.concat(
        mapMonovalentProductivities(forecast),
        mapPolivalentProductivities(forecast, polyvalentProductivities)
    ).collect(Collectors.toList());
  }

  private Stream<HeadcountProductivity> mapMonovalentProductivities(final Forecast forecast) {
    return data.stream().map(productivityData -> createRegularProductivity(forecast, productivityData));
  }

  private Stream<HeadcountProductivity> mapPolivalentProductivities(
      final Forecast forecast,
      final List<PolyvalentProductivityRequest> polyvalentProductivities
  ) {
    final var polyvalentProductivity = polyvalentProductivities.stream()
        .filter(pp -> pp.getProcessName() == processName)
        .findFirst();

    return polyvalentProductivity.map(pp ->
        data.stream()
            .map(productivityData -> createPolyvalentProductivities(forecast, pp, productivityData))
    ).orElseGet(Stream::empty);
  }

  private HeadcountProductivity createRegularProductivity(final Forecast forecast, final HeadcountProductivityDataRequest data) {
    return HeadcountProductivity.builder()
        .productivityMetricUnit(productivityMetricUnit)
        .processPath(processPath)
        .processName(processName)
        .abilityLevel(abilityLevel)
        .date(data.getDayTime())
        .productivity(data.getProductivity())
        .forecast(forecast)
        .build();
  }

  private HeadcountProductivity createPolyvalentProductivities(
      final Forecast forecast,
      final PolyvalentProductivityRequest polyvalentProductivity,
      final HeadcountProductivityDataRequest productivityData
  ) {
    return polyvalentProductivity.toHeadcountProductivity(
        forecast,
        processPath,
        productivityData.getProductivity(),
        productivityData.getDayTime()
    );
  }

  @Override
  public String getHeadcountType() {
    return null;
  }

  @Override
  public ProcessingType getType() {
    return ProcessingType.PRODUCTIVITY;
  }
}
