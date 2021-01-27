package com.mercadolibre.planning.model.api.gateway;

import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;

import java.util.List;

public interface PlanningDistributionGateway {

    void create(final List<PlanningDistribution> entities, final long forecastId);
}
