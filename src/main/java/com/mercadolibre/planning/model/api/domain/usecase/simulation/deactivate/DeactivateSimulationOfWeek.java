package com.mercadolibre.planning.model.api.domain.usecase.simulation.deactivate;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.ZonedDateTime;
import lombok.Value;

@Value
public class DeactivateSimulationOfWeek {

    String logisticCenterId;

    Workflow workflow;

    ZonedDateTime dateFrom;

    ZonedDateTime dateTo;

    long userId;

}
