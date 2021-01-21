package com.mercadolibre.planning.model.api.web.controller.forecast.request;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import lombok.Value;

import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;

@Value
public class PolyvalentProductivityRequest {

    @NotNull
    private ProcessName processName;

    @NotNull
    private MetricUnit productivityMetricUnit;

    private long productivity;

    private int abilityLevel;

    public HeadcountProductivity toHeadcountProductivity(final Forecast forecast,
                                                         final long regularProductivity,
                                                         final ZonedDateTime date) {
        return HeadcountProductivity.builder()
                .abilityLevel(abilityLevel)
                .processName(processName)
                .productivityMetricUnit(UNITS_PER_HOUR)
                .date(date)
                .productivity((regularProductivity * productivity) / 100)
                .forecast(forecast)
                .build();
    }
}
