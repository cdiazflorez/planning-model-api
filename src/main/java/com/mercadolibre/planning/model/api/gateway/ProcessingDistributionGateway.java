package com.mercadolibre.planning.model.api.gateway;

import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;

import java.util.List;

public interface ProcessingDistributionGateway {

    void create(final List<ProcessingDistribution> entities, final long forecastId);
}
