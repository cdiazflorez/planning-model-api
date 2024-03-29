package com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt;

import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class Backlog {

    private ZonedDateTime date;

    private int quantity;
}
