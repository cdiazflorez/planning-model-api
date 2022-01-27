package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import lombok.Value;

import java.util.Date;

@Value
public class PlanningDistributionViewImpl implements PlanningDistributionElemView {
    private long forecastId;

    private Date dateIn;

    private Date dateOut;

    private long quantity;

    private String quantityMetricUnit;

    public static PlanningDistributionElemView fromWithQuantity(
            final PlanningDistributionElemView original,
            final long quantity
    ) {
        return new PlanningDistributionViewImpl(
                original.getForecastId(),
                original.getDateIn(),
                original.getDateOut(),
                quantity,
                original.getQuantityMetricUnit()
        );
    }
}
