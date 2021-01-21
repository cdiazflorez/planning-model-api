package com.mercadolibre.planning.model.api.domain.usecase.capacity;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@AllArgsConstructor
public class CapacityOutput {
    private ZonedDateTime date;
    private MetricUnit metricUnit;
    private long value;

}
