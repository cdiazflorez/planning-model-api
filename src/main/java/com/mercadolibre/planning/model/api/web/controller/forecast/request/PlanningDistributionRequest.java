package com.mercadolibre.planning.model.api.web.controller.forecast.request;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Value;


@Value
public class PlanningDistributionRequest {

  @NotNull ZonedDateTime dateIn;

  @NotNull ZonedDateTime dateOut;

  @NotNull MetricUnit quantityMetricUnit;

  ProcessPath processPath;

  double quantity;

  @NotEmpty
  @Valid List<MetadataRequest> metadata;

  public PlanningDistribution toPlanningDistribution(final Forecast forecast) {
    return PlanningDistribution.builder()
        .dateIn(dateIn)
        .dateOut(dateOut)
        .processPath(processPath)
        .forecast(forecast)
        .quantity(quantity)
        .quantityMetricUnit(quantityMetricUnit)
        .metadatas(metadata.stream()
                       .map(MetadataRequest::toPlanningDistributionMetadata)
                       .collect(Collectors.toList()))
        .build();
  }
}
