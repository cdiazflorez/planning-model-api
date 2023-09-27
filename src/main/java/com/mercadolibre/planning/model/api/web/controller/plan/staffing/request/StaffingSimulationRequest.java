package com.mercadolibre.planning.model.api.web.controller.plan.staffing.request;

import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class StaffingSimulationRequest {

  @NotNull
  List<Simulation> simulations;
}
