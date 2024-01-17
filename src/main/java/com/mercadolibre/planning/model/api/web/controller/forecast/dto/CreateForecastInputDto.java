package com.mercadolibre.planning.model.api.web.controller.forecast.dto;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.HeadcountDistributionRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.MetadataRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.PlanningDistributionRequest;
import java.util.List;


public record CreateForecastInputDto(

    String week,

    long userId,
    Workflow workflow,
    String logisticCenterId,
    List<MetadataRequest> metadata,
    List<HeadcountDistributionRequest> headcountDistributions,
    List<StaffingPlanDto> staffingPlan,
    List<PlanningDistributionRequest> planningDistributions
) {
}
