package com.mercadolibre.planning.model.api.domain.usecase.input;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import lombok.Value;

@Value
public class ConfigurationInput {

    private String logisticCenterId;
    private String key;
    private long value;
    private MetricUnit metricUnit;
}
