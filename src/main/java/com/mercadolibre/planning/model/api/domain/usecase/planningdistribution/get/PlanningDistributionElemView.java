package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import java.util.Date;

public interface PlanningDistributionElemView {

    long getForecastId();

    Date getDateIn();

    Date getDateOut();

    long getQuantity();

    String getQuantityMetricUnit();
}
