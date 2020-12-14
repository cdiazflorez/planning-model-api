package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.SuggestedWavePlanningDistributionView;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SuggestedWavePlanningDistributionViewImpl
        implements SuggestedWavePlanningDistributionView {
    private final long quantity;

    @Override
    public long getQuantity() {
        return quantity;
    }
}
