package com.mercadolibre.planning.model.api.domain.usecase.cycletime.get;

import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
public class GetCycleTimeInput {

    private String logisticCenterId;

    private List<ZonedDateTime> cptDates;
}
