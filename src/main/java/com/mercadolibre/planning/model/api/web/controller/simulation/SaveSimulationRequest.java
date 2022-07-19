package com.mercadolibre.planning.model.api.web.controller.simulation;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.activate.SimulationInput;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class SaveSimulationRequest {

  @NotNull
  String warehouseId;

  @NotNull
  List<Simulation> simulations;

  @NotNull
  Long userId;

  public SimulationInput toSimulationInput(final Workflow workflow) {
    return SimulationInput.builder()
        .warehouseId(warehouseId)
        .workflow(workflow)
        .simulations(simulations)
        .userId(userId)
        .build();
  }
}
