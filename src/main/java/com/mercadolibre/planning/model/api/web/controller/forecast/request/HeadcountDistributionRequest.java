package com.mercadolibre.planning.model.api.web.controller.forecast.request;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountDistribution;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Value
public class HeadcountDistributionRequest {

    @NotNull
    private ProcessName processName;

    @NotNull
    private MetricUnit quantityMetricUnit;

    @NotEmpty
    @Valid
    private List<AreaRequest> areas;

    public List<HeadcountDistribution> toHeadcountDists(final Forecast forecast) {
        final List<HeadcountDistribution> headcountDistributions = new ArrayList<>();
        areas.forEach(area -> headcountDistributions.add(
                HeadcountDistribution.builder()
                        .processName(processName)
                        .quantityMetricUnit(quantityMetricUnit)
                        .quantity(area.getQuantity())
                        .area(area.getAreaId())
                        .forecast(forecast)
                        .build()));

        return headcountDistributions;
    }
}
