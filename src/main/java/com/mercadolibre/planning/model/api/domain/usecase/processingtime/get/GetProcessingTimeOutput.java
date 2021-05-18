package com.mercadolibre.planning.model.api.domain.usecase.processingtime.get;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GetProcessingTimeOutput {

    private Workflow workflow;

    private String logisticCenterId;

    private long value;

    private MetricUnit metricUnit;

}
