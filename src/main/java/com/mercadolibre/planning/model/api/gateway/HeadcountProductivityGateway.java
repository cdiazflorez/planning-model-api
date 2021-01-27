package com.mercadolibre.planning.model.api.gateway;

import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;

import java.util.List;

public interface HeadcountProductivityGateway {

    void create(final List<HeadcountProductivity> entities, final long forecastId);
}
