package com.mercadolibre.planning.model.api.domain.usecase.simulation;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class SimulationOutput {

    private long id;
    private ZonedDateTime date;
    private Workflow workflow;
    private ProcessName processName;
    private long quantity;
    private MetricUnit quantityMetricUnit;
    private boolean isActive;
    private Integer abilityLevel;
}
