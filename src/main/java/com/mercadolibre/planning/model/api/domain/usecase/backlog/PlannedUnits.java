package com.mercadolibre.planning.model.api.domain.usecase.backlog;

import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class PlannedUnits {
    private ZonedDateTime dateIn;

    private ZonedDateTime dateOut;

    private long total;
}
