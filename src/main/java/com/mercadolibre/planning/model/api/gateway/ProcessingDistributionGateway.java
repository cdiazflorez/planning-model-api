package com.mercadolibre.planning.model.api.gateway;

import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.entity.plan.StaffingPlan;
import com.mercadolibre.planning.model.api.domain.entity.plan.StaffingPlanInput;
import java.util.List;

public interface ProcessingDistributionGateway {

    void create(final List<ProcessingDistribution> entities, final long forecastId);

    List<StaffingPlan> getStaffingPlan(StaffingPlanInput staffingPlanInput);

}
