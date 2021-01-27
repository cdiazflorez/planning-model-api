package com.mercadolibre.planning.model.api.gateway;

import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadata;

import java.util.List;

public interface ForecastGateway {

    Forecast create(final Forecast forecast, final List<ForecastMetadata> metadatas);
}
