package com.mercadolibre.planning.model.api.web.controller.forecast.request;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class ProcessingDistributionRequest {
  @NotNull
  ProcessPath processPath;

  @NotNull
  ProcessName processName;

  @NotNull
  ProcessingType type;

  @NotNull
  MetricUnit quantityMetricUnit;

  @NotEmpty
  @Valid
  List<ProcessingDistributionDataRequest> data;

  public List<ProcessingDistribution> toProcessingDistributions(final Forecast forecast) {
    final List<ProcessingDistribution> processingDistributions = new ArrayList<>();
    data.forEach(pddr -> processingDistributions.add(
            ProcessingDistribution.builder()
                .processPath(processPath)
                .processName(processName)
                .quantityMetricUnit(quantityMetricUnit)
                .type(type)
                .date(pddr.getDate())
                .forecast(forecast)
                .quantity(pddr.getQuantity())
                .build()
        )
    );

    return processingDistributions;
  }
}
