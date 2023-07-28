package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class GetPlanningDistributionOutput {

    Instant dateIn;

    Instant dateOut;

    MetricUnit metricUnit;

    ProcessPath processPath;

    double total;
}
