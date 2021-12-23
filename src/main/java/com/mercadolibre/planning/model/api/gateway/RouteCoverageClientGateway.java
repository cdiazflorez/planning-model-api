package com.mercadolibre.planning.model.api.gateway;

import com.mercadolibre.planning.model.api.domain.entity.sla.RouteCoverageResult;

import java.util.List;


public interface RouteCoverageClientGateway {
    List<RouteCoverageResult> get(String warehouse);
}
