package com.mercadolibre.planning.model.api.domain.entity.sla;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import lombok.Value;

@Value
public class ProcessingTime {

    private long value;

    private MetricUnit unitMetric;
}
