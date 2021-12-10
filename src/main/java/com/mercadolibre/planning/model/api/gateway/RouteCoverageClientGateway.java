package com.mercadolibre.planning.model.api.gateway;

import com.mercadolibre.planning.model.api.client.rest.config.CacheConfig;
import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.RouteCoverageResult;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;


public interface RouteCoverageClientGateway {
    @Cacheable(CacheConfig.CPT_CACHE_NAME)
    List<RouteCoverageResult> get(String warehouse);
}
