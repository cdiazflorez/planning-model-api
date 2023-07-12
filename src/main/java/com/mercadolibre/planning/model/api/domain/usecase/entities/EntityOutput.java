package com.mercadolibre.planning.model.api.domain.usecase.entities;

import static com.mercadolibre.planning.model.api.util.DateUtils.fromDate;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode
public class EntityOutput {

  private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

  private Workflow workflow;

  private ZonedDateTime date;

  private ProcessPath processPath;

  private ProcessName processName;

  private ProcessingType type;

  private MetricUnit metricUnit;

  private Source source;

  private double value;

  public double getValue() {
    return Double.parseDouble(DECIMAL_FORMAT.format(value));
  }

  @JsonIgnore
  public long getRoundedValue() {
    return Math.round(value);
  }

  public static EntityOutput fromProcessingDistributionView(
      final ProcessingDistributionView processingDistributionView,
      final Workflow workflow) {
    return EntityOutput.builder()
        .workflow(workflow)
        .source(FORECAST)
        .processName(processingDistributionView.getProcessName())
        .type(processingDistributionView.getType())
        .date(fromDate(processingDistributionView.getDate()))
        .metricUnit(processingDistributionView.getQuantityMetricUnit())
        .value(processingDistributionView.getQuantity())
        .build();
  }
}
