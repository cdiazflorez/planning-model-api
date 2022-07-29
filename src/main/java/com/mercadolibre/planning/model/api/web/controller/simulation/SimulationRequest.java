package com.mercadolibre.planning.model.api.web.controller.simulation;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.activate.SimulationInput;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;

import lombok.Data;

import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.List;

@Data
public class SimulationRequest {

    @NotNull
    private String warehouseId;

    @NotNull
    private List<ProcessName> processName;

    @NotNull
    private ZonedDateTime dateFrom;

    @NotNull
    private ZonedDateTime dateTo;

    @NotNull
    private List<QuantityByDate> backlog;

    @NotNull
    private List<Simulation> simulations;

    @NotNull
    private Long userId;

    @NotNull
    private String timeZone;

    private boolean applyDeviation;

    public SimulationInput toSimulationInput(final Workflow workflow) {
        return SimulationInput.builder()
                .warehouseId(warehouseId)
                .workflow(workflow)
                .simulations(simulations)
                .userId(userId)
                .build();
    }

}
