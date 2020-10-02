package com.mercadolibre.planning.model.api.domain.usecase.output;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.request.Source;

import java.time.ZonedDateTime;

public interface EntityOutput {

    Workflow getWorkflow();

    ZonedDateTime getDate();

    ProcessName getProcessName();

    MetricUnit getMetricUnit();

    Source getSource();
}
