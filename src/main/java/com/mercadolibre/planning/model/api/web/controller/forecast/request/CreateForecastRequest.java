package com.mercadolibre.planning.model.api.web.controller.forecast.request;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.create.CreateForecastInput;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import java.util.List;

@Value
public class CreateForecastRequest {

    @NotEmpty
    @Valid
    private List<MetadataRequest> metadata;

    @NotEmpty
    @Valid
    private List<ProcessingDistributionRequest> processingDistributions;

    @NotEmpty
    @Valid
    private List<HeadcountDistributionRequest> headcountDistributions;

    @NotEmpty
    @Valid
    private List<PolyvalentProductivityRequest> polyvalentProductivities;

    @NotEmpty
    @Valid
    private List<HeadcountProductivityRequest> headcountProductivities;

    @NotEmpty
    @Valid
    private List<PlanningDistributionRequest> planningDistributions;

    @NotEmpty
    @Valid
    private List<ProcessingDistributionRequest> backlogLimits;

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
                .build();
    }
}

