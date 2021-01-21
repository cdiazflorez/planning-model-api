package com.mercadolibre.planning.model.api.web.controller.request;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Value
public class HeadcountProductivityRequest {

    @NotNull
    private ProcessName processName;

    @NotNull
    private MetricUnit productivityMetricUnit;

    private int abilityLevel;

    @NotEmpty
    @Valid
    private List<HeadcountProductivityDataRequest> data;

    public Set<HeadcountProductivity> toHeadcountProductivities(
            final Forecast forecast,
            final List<PolyvalentProductivityRequest> polyvalentProductivities) {

        final Set<HeadcountProductivity> headcountProductivities = new HashSet<>();

        data.forEach(productivityData -> {
            headcountProductivities.add(createRegularProductivity(forecast, productivityData));
            headcountProductivities.addAll(createPolyvalentProductivities(forecast,
                    polyvalentProductivities, productivityData));
        });

        return headcountProductivities;
    }

    private HeadcountProductivity createRegularProductivity(
            final Forecast forecast, final HeadcountProductivityDataRequest data) {

        return HeadcountProductivity.builder()
                .productivityMetricUnit(productivityMetricUnit)
                .processName(processName)
                .abilityLevel(abilityLevel)
                .date(data.getDayTime())
                .productivity(data.getProductivity())
                .forecast(forecast)
                .build();
    }

    private Set<HeadcountProductivity> createPolyvalentProductivities(
            final Forecast forecast,
            final List<PolyvalentProductivityRequest> polyvalentProductivityRequests,
            final HeadcountProductivityDataRequest productivityData) {

        final Set<HeadcountProductivity> headcountProductivities = new HashSet<>();

        polyvalentProductivityRequests
                .stream()
                .filter(polyProd -> polyProd.getProcessName() == processName)
                .forEach(polyProd ->
                        headcountProductivities.add(polyProd.toHeadcountProductivity(
                                forecast,
                                productivityData.getProductivity(),
                                productivityData.getDayTime())
                        ));

        return headcountProductivities;
    }
}
