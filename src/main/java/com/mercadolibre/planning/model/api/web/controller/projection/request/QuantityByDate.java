package com.mercadolibre.planning.model.api.web.controller.projection.request;

import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuantityByDate {

    @NotNull
    private ZonedDateTime date;

    private int quantity;

    public Backlog toBacklog() {
        return new Backlog(date, quantity);
    }
}
