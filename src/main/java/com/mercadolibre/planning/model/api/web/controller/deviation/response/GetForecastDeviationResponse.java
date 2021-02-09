package com.mercadolibre.planning.model.api.web.controller.deviation.response;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class GetForecastDeviationResponse {

    private ZonedDateTime dateFrom;
    private ZonedDateTime dateTo;
    private double value;
    private MetricUnit metricUnit;

}
