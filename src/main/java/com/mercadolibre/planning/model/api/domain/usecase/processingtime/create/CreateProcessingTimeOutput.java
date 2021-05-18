package com.mercadolibre.planning.model.api.domain.usecase.processingtime.create;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.processingtime.response.CreateProcessingTimeResponse;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Builder
@Value
public class CreateProcessingTimeOutput {

    private final long id;

    private Workflow workflow;

    private String logisticCenterId;

    private int value;

    private MetricUnit metricUnit;

    private ZonedDateTime cptFrom;

    private ZonedDateTime cptTo;

    private ZonedDateTime dateCreated;

    private ZonedDateTime lastUpdated;

    private Long userId;

    public CreateProcessingTimeResponse toCreateProcessingTimeResponse(
            final CreateProcessingTimeOutput request) {

        return CreateProcessingTimeResponse.builder()
                .id(request.getId())
                .value(request.getValue())
                .workflow(request.getWorkflow())
                .logisticCenterId(request.getLogisticCenterId())
                .metricUnit(request.getMetricUnit())
                .cptFrom(request.getCptFrom())
                .cptTo(request.getCptTo())
                .build();
    }
}
