package com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import lombok.Value;

@Value
public class ProcessingTime {

    private long value;

    private MetricUnit unitMetric;
}
