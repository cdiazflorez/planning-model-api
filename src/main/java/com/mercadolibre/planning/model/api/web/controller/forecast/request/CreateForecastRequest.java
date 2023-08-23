package com.mercadolibre.planning.model.api.web.controller.forecast.request;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.create.CreateForecastInput;
import lombok.Value;

import java.util.List;

@Value
public class CreateForecastRequest {

    String logisticCenterId;

    List<MetadataRequest> metadata;

    List<ProcessingDistributionRequest> processingDistributions;

    List<HeadcountDistributionRequest> headcountDistributions;

    List<PolyvalentProductivityRequest> polyvalentProductivities;

    List<HeadcountProductivityRequest> headcountProductivities;

    List<PlanningDistributionRequest> planningDistributions;

    List<ProcessingDistributionRequest> backlogLimits;

    String week;

    long userId;

    public CreateForecastInput toCreateForecastInput(final Workflow workflow) {
        return CreateForecastInput.builder()
                .workflow(workflow)
                .logisticCenterId(logisticCenterId)
                .headcountDistributions(headcountDistributions)
                .headcountProductivities(headcountProductivities)
                .metadata(metadata)
                .planningDistributions(planningDistributions)
                .polyvalentProductivities(polyvalentProductivities)
                .processingDistributions(processingDistributions)
                .backlogLimits(backlogLimits)
                .week(week)
                .userId(userId)
                .build();
    }
}

