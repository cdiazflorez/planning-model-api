package com.mercadolibre.planning.model.api.domain.usecase.simulation.activate;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class SimulationOutput {

    long id;
    ZonedDateTime date;
    Workflow workflow;
    ProcessName processName;
    double quantity;
    MetricUnit quantityMetricUnit;
    boolean isActive;
    Integer abilityLevel;
}
