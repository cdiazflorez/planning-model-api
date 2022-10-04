package com.mercadolibre.planning.model.api.domain.usecase.simulation.deactivate;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.Value;

@Value
public class DeactivateSimulationInput {

    String logisticCenterId;

    Workflow workflow;

    ProcessName processName;

    List<ZonedDateTime> dates;

    long userId;

}
