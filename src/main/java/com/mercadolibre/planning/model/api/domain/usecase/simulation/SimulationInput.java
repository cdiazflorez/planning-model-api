package com.mercadolibre.planning.model.api.domain.usecase.simulation;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.request.QuantityByDate;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
public class SimulationInput {

    private String warehouseId;
    private Workflow workflow;
    private List<ProcessName> processName;
    private ZonedDateTime dateFrom;
    private ZonedDateTime dateTo;
    private List<QuantityByDate> backlog;
    private List<Simulation> simulations;
}
