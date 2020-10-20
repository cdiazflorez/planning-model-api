package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import java.time.ZonedDateTime;

public interface PlanningDistributionView {

    ZonedDateTime getDateIn();

    ZonedDateTime getDateOut();

    long getQuantity();

    String getQuantityMetricUnit();
}
