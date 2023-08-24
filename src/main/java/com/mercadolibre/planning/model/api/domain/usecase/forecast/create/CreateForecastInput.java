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
    Workflow workflow;
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
}
