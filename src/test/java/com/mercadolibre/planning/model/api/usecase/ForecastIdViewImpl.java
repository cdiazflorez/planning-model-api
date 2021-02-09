package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastIdView;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ForecastIdViewImpl implements ForecastIdView {

    private Long id;

    @Override
    public Long getId() {
        return id;
    }
}
