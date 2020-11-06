package com.mercadolibre.planning.model.api.web.controller.configuration;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.usecase.input.ConfigurationInput;
import lombok.Value;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Value
public class CreateConfigurationRequest {

    @NotEmpty
    private String logisticCenterId;

    @NotEmpty
    private String key;

    @NotNull
    private Long value;

    @NotNull
    private MetricUnit metricUnit;

    public ConfigurationInput toConfigurationInput() {
        return new ConfigurationInput(logisticCenterId, key, value, metricUnit);
    }
}
