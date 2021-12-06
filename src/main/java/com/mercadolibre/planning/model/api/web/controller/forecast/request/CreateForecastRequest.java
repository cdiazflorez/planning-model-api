package com.mercadolibre.planning.model.api.web.controller.forecast.request;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.create.CreateForecastInput;
import lombok.Value;

import java.util.List;

@Value
public class CreateForecastRequest {

    private List<MetadataRequest> metadata;

    private List<ProcessingDistributionRequest> processingDistributions;

    private List<HeadcountDistributionRequest> headcountDistributions;

    private List<PolyvalentProductivityRequest> polyvalentProductivities;

    private List<HeadcountProductivityRequest> headcountProductivities;

    private List<PlanningDistributionRequest> planningDistributions;

    private List<ProcessingDistributionRequest> backlogLimits;

    private long userId;

    public CreateForecastInput toCreateForecastInput(final Workflow workflow) {
        return CreateForecastInput.builder()
                .workflow(workflow)
                .headcountDistributions(headcountDistributions)
                .headcountProductivities(headcountProductivities)
                .metadata(metadata)
                .planningDistributions(planningDistributions)
                .polyvalentProductivities(polyvalentProductivities)
                .processingDistributions(processingDistributions)
                .backlogLimits(backlogLimits)
                .userId(userId)
                .build();
    }
}

