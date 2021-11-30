package com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get;

import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class MaxCapacityOutput {

    public String logisticCenterId;

    public ZonedDateTime loadDate;

    public ZonedDateTime maxCapacityDate;

    public long maxCapacityValue;
}
