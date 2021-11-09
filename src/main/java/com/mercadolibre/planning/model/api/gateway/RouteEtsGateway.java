package com.mercadolibre.planning.model.api.gateway;

import com.mercadolibre.planning.model.api.client.rest.config.CacheConfig;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsDto;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsRequest;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

public interface RouteEtsGateway {

    @Cacheable(CacheConfig.CPT_CACHE_NAME)
    List<RouteEtsDto> postRoutEts(final RouteEtsRequest routeEtsRequest);
}
