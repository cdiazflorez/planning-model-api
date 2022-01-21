package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
@AllArgsConstructor
public class GetPlanningDistributionOutput {

    private ZonedDateTime dateIn;

    private ZonedDateTime dateOut;

    private MetricUnit metricUnit;

    private long total;

    private boolean isDeferred;
}
