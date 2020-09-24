package com.mercadolibre.planning.model.api.domain.usecase.output;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.request.Source;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;

@AllArgsConstructor
@Getter
@Builder
public class GetEntityOutput {

    private Workflow workflow;
    private ZonedDateTime date;
    private ProcessName processName;
    private double value;
    private MetricUnit metricUnit;
    private Source source;
}
