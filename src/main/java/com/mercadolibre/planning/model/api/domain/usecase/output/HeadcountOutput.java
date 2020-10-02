package com.mercadolibre.planning.model.api.domain.usecase.output;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.request.Source;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@AllArgsConstructor
@Builder
public class HeadcountOutput implements EntityOutput {

    private Workflow workflow;
    private ZonedDateTime date;
    private ProcessName processName;
    private MetricUnit metricUnit;
    private Source source;
    private long value;
}
