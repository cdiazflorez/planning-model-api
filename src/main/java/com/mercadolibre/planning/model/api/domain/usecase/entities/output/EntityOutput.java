package com.mercadolibre.planning.model.api.domain.usecase.entities.output;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.request.Source;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;

@Getter
@SuperBuilder
@EqualsAndHashCode
public class EntityOutput {

    private Workflow workflow;
    private ZonedDateTime date;
    private ProcessName processName;
    private ProcessingType type;
    private MetricUnit metricUnit;
    private Source source;
    private long value;
}
