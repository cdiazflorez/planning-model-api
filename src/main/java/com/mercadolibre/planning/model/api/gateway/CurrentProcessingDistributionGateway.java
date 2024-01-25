package com.mercadolibre.planning.model.api.gateway;

import com.mercadolibre.planning.model.api.domain.entity.plan.CurrentStaffingPlanInput;
import com.mercadolibre.planning.model.api.domain.entity.plan.StaffingPlan;
import java.util.List;

/**
 * Gateway to get current staffing plan.
 */
public interface CurrentProcessingDistributionGateway {

  /**
   * Get current staffing plan.
   *
   * @param input {@link CurrentStaffingPlanInput} current staffing plan input.
   * @return {@link StaffingPlan} current staffing plan list.
   */
  List<StaffingPlan> getCurrentStaffingPlan(CurrentStaffingPlanInput input);

}
