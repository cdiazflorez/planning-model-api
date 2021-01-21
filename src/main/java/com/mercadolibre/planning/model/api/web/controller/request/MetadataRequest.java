package com.mercadolibre.planning.model.api.web.controller.request;

import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionMetadata;
import lombok.Value;

import javax.validation.constraints.NotBlank;

@Value
public class MetadataRequest {

    @NotBlank
    private String key;

    @NotBlank
    private String value;

    public ForecastMetadata toForecastMetadata(final long forecastId) {
        return new ForecastMetadata(forecastId, key, value);
    }

    public PlanningDistributionMetadata toPlanningDistributionMetadata(final long id) {
        return PlanningDistributionMetadata.builder()
                .planningDistributionId(id)
                .key(key)
                .value(value)
                .build();
    }
}

