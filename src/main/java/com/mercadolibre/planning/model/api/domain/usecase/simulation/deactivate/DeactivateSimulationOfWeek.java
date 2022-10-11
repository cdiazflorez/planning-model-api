package com.mercadolibre.planning.model.api.domain.usecase.simulation.deactivate;

import java.time.ZonedDateTime;
import lombok.Value;

@Value
public class DeactivateSimulationOfWeek {

    String logisticCenterId;

    ZonedDateTime dateFrom;

    ZonedDateTime dateTo;

    long userId;

}
