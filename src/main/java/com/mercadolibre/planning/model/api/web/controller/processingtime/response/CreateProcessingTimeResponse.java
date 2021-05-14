package com.mercadolibre.planning.model.api.web.controller.processingtime.response;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Builder
@Value
public class CreateProcessingTimeResponse {

    private final long id;

    private Workflow workflow;

    private String logisticCenterId;

    private int value;

    private MetricUnit metricUnit;

    private ZonedDateTime cptFrom;

    private ZonedDateTime cptTo;

    private boolean isActive;
}
