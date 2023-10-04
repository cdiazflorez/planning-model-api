package com.mercadolibre.planning.model.api.web.controller.plan.staffing.request;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.activate.SimulationInput;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdateStaffingPlanRequest {

  @NotNull
  List<Simulation> processesToUpdate;

  public SimulationInput toSimulationInput(final Workflow workflow, final String warehouseId, final Long userId) {
    return SimulationInput.builder()
        .warehouseId(warehouseId)
        .workflow(workflow)
        .simulations(processesToUpdate)
        .userId(userId)
        .build();
  }
}
