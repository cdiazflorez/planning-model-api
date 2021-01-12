package com.mercadolibre.planning.model.api.web.controller.forecast.request;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.List;


@Value
public class PlanningDistributionRequest {

    @NotNull
    private ZonedDateTime dateIn;

    @NotNull
    private ZonedDateTime dateOut;

    @NotNull
    private MetricUnit quantityMetricUnit;

    private long quantity;

    @NotEmpty
    @Valid
    private List<MetadataRequest> metadata;

    public PlanningDistribution toPlanningDistribution(final Forecast forecast) {
        return PlanningDistribution.builder()
                .dateIn(dateIn)
                .dateOut(dateOut)
                .forecast(forecast)
                .quantity(quantity)
                .quantityMetricUnit(quantityMetricUnit)
                .build();
    }

}
