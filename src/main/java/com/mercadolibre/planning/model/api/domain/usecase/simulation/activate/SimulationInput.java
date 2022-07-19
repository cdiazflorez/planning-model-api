package com.mercadolibre.planning.model.api.domain.usecase.simulation.activate;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import lombok.Builder;
import lombok.Value;
import java.util.List;

@Value
@Builder
public class SimulationInput {

    private String warehouseId;
    private Workflow workflow;
    private List<Simulation> simulations;
    private long userId;
}
