package com.mercadolibre.planning.model.api.web.controller.processingtime.request;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.processingtime.create.CreateProcessingTimeInput;
import lombok.Value;

import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;

@Value
public class CreateProcessingTimeRequest {

    @NotNull
    private int value;

    @NotNull
    private MetricUnit metricUnit;

    @NotNull
    private String logisticCenterId;

    @NotNull
    private Workflow workflow;

    @NotNull
    private ZonedDateTime cptFrom;

    @NotNull
    private ZonedDateTime cptTo;

    @NotNull
    private long userId;

    public CreateProcessingTimeInput toCreateForecastInput(
            final CreateProcessingTimeRequest request) {

        return CreateProcessingTimeInput.builder()
                .value(request.getValue())
                .workflow(request.getWorkflow())
                .logisticCenterId(request.getLogisticCenterId())
                .metricUnit(request.getMetricUnit())
                .cptFrom(request.getCptFrom())
                .cptTo(request.getCptTo())
                .userId(request.getUserId())
                .build();
    }
}
