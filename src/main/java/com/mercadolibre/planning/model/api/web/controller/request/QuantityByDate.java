package com.mercadolibre.planning.model.api.web.controller.request;

import com.mercadolibre.planning.model.api.domain.usecase.projection.Backlog;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;

@Data
@AllArgsConstructor
public class QuantityByDate {

    @NotNull
    private ZonedDateTime date;

    private int quantity;

    public Backlog toBacklog() {
        return new Backlog(date, quantity);
    }
}
