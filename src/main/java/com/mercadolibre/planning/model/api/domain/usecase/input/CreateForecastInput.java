package com.mercadolibre.planning.model.api.domain.usecase.input;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.request.HeadcountDistributionRequest;
import com.mercadolibre.planning.model.api.web.controller.request.HeadcountProductivityRequest;
import com.mercadolibre.planning.model.api.web.controller.request.MetadataRequest;
import com.mercadolibre.planning.model.api.web.controller.request.PlanningDistributionRequest;
import com.mercadolibre.planning.model.api.web.controller.request.PolyvalentProductivityRequest;
import com.mercadolibre.planning.model.api.web.controller.request.ProcessingDistributionRequest;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class CreateForecastInput {

    private Workflow workflow;
    private List<MetadataRequest> metadata;
    private List<ProcessingDistributionRequest> processingDistributions;
    private List<HeadcountDistributionRequest> headcountDistributions;
    private List<PolyvalentProductivityRequest> polyvalentProductivities;
    private List<HeadcountProductivityRequest> headcountProductivities;
    private List<PlanningDistributionRequest> planningDistributions;
}
