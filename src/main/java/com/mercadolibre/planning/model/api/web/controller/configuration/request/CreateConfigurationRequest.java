package com.mercadolibre.planning.model.api.web.controller.configuration.request;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.create.ConfigurationInput;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class CreateConfigurationRequest {

  @NotEmpty
  String logisticCenterId;

  @NotEmpty
  String key;

  @NotNull
  Long value;

  @NotNull
  MetricUnit metricUnit;

  public ConfigurationInput toConfigurationInput(final Long userId) {
    return new ConfigurationInput(logisticCenterId, key, value, metricUnit, userId);
  }
}
