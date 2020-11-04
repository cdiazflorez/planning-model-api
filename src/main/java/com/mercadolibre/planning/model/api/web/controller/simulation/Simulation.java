package com.mercadolibre.planning.model.api.web.controller.simulation;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;

import java.util.List;

@Data
@AllArgsConstructor
public class Simulation {

    @NotNull
    private ProcessName processName;

    @NotNull
    private List<SimulationEntity> entities;
}
