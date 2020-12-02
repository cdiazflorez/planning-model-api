package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastMetadataView;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ForecastMetadataViewImpl implements ForecastMetadataView {

    private String key;

    private String value;

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getValue() {
        return value;
    }
}
