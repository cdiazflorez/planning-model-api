package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import lombok.AllArgsConstructor;

import java.time.ZonedDateTime;

@AllArgsConstructor
public class PlanningDistributionViewImpl implements PlanningDistributionView {

    private ZonedDateTime dateIn;

    private ZonedDateTime dateOut;

    private long quantity;

    private MetricUnit quantityMetricUnit;

    @Override
    public ZonedDateTime getDateIn() {
        return dateIn;
    }

    @Override
    public ZonedDateTime getDateOut() {
        return dateOut;
    }

    @Override
    public long getQuantity() {
        return quantity;
    }

    @Override
    public String getQuantityMetricUnit() {
        return quantityMetricUnit.toString();
    }
}
