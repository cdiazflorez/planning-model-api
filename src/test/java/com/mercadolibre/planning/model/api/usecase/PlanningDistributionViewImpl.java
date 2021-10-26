package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import lombok.AllArgsConstructor;

import java.util.Date;

@AllArgsConstructor
public class PlanningDistributionViewImpl implements PlanningDistributionView {

    private long forecastId;

    private Date dateIn;

    private Date dateOut;

    private long quantity;

    private MetricUnit quantityMetricUnit;

    @Override
    public long getForecastId() {
        return forecastId;
    }

    @Override
    public Date getDateIn() {
        return dateIn;
    }

    @Override
    public Date getDateOut() {
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
