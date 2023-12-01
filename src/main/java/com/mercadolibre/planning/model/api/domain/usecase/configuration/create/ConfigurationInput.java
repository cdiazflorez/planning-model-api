package com.mercadolibre.planning.model.api.domain.usecase.configuration.create;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import lombok.Value;

@Value
public class ConfigurationInput {

    String logisticCenterId;
    String key;
    long value;
    MetricUnit metricUnit;
    long userId;
}
