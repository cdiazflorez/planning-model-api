package com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteEtsDto {

    private String id;

    private String from;

    private String canalization;

    private String serviceId;

    private FixedEtsByDayDto fixedEtsByDay;

    private Date dateCreated;

    private Date lastUpdated;
}
