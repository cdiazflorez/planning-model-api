package com.mercadolibre.planning.model.api.gateway;

import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountDistribution;

import java.util.List;

public interface HeadcountDistributionGateway {

    void create(final List<HeadcountDistribution> entities, final long forecastId);
}
