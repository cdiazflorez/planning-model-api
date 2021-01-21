package com.mercadolibre.planning.model.api.web.controller.configuration;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.usecase.input.ConfigurationInput;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
public class UpdateConfigurationRequest {

    @NotNull
    private Long value;

    @NotNull
    private MetricUnit metricUnit;

    public ConfigurationInput toConfigurationInput(final String logisticCenterId,
                                                   final String key) {
        return new ConfigurationInput(logisticCenterId, key, value, metricUnit);
    }
}
