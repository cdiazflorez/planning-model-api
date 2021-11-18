package com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import lombok.Value;

@Value
public class ProcessingTime {

    private long value;

    private MetricUnit unitMetric;
}
