package com.mercadolibre.planning.model.api.web.controller.plan.staffing.request;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.update.UpdateStaffingPlanInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.update.UpdateStaffingPlanInput.Resource;
import java.util.List;

public record StaffingPlanUpdateRequest(
    List<Resource> resources
) {

  public UpdateStaffingPlanInput toUpdateStaffingPlanInput(
      final String logisticCenterId,
      final Workflow workflow,
      final long userId
  ) {
    return new UpdateStaffingPlanInput(
        logisticCenterId,
        workflow,
        userId,
        resources
    );
  }
}
