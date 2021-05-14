package com.mercadolibre.planning.model.api.domain.usecase.processingtime.create;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;

@Builder
@Value
public class CreateProcessingTimeInput {

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
}
