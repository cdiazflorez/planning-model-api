package com.mercadolibre.planning.model.api.domain.usecase.forecast.create;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.HeadcountDistributionRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.HeadcountProductivityRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.MetadataRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.PlanningDistributionRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.PolyvalentProductivityRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.ProcessingDistributionRequest;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class CreateForecastInput {

    private Workflow workflow;
    private String logisticCenterId;
    private List<MetadataRequest> metadata;
    private List<ProcessingDistributionRequest> processingDistributions;
    private List<HeadcountDistributionRequest> headcountDistributions;
    private List<PolyvalentProductivityRequest> polyvalentProductivities;
    private List<HeadcountProductivityRequest> headcountProductivities;
    private List<PlanningDistributionRequest> planningDistributions;
    private List<ProcessingDistributionRequest> backlogLimits;
    private String week;
    private long userId;
}
