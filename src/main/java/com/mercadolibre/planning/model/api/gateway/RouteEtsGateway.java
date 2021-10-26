package com.mercadolibre.planning.model.api.gateway;

import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsDto;

import java.util.List;

public interface RouteEtsGateway {

    List<RouteEtsDto> getRoutEts(final String warehouseId);
}
