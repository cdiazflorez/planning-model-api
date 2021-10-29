package com.mercadolibre.planning.model.api.gateway;

import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsDto;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsRequest;

import java.util.List;

public interface RouteEtsGateway {

    List<RouteEtsDto> postRoutEts(final RouteEtsRequest routeEtsRequest);
}
