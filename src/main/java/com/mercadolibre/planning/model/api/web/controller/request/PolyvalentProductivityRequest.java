package com.mercadolibre.planning.model.api.web.controller.request;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import lombok.Value;

import javax.validation.constraints.NotNull;

import java.time.OffsetTime;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.util.DateUtils.getUtcOffset;

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
                                                         final OffsetTime dayTime) {
        return HeadcountProductivity.builder()
                .abilityLevel(abilityLevel)
                .processName(processName)
                .productivityMetricUnit(UNITS_PER_HOUR)
                .dayTime(getUtcOffset(dayTime))
                .productivity((regularProductivity * productivity) / 100)
                .forecast(forecast)
                .build();
    }
}
