package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import java.util.Date;

public interface PlanningDistributionView {

    long getForecastId();

    Date getDateIn();

    Date getDateOut();

    long getQuantity();

    String getQuantityMetricUnit();
}
