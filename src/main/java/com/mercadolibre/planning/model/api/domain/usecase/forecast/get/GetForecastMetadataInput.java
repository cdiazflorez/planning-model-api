package com.mercadolibre.planning.model.api.domain.usecase.forecast.get;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
@Builder
public class GetForecastMetadataInput {

    private List<Long> forecastIds;
    private ZonedDateTime dateFrom;
    private ZonedDateTime dateTo;

}
