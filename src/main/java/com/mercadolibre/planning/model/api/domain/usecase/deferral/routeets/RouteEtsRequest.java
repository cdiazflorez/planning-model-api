package com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@SuperBuilder
@EqualsAndHashCode
public class RouteEtsRequest {
    private List<String> fromFilter;
}
