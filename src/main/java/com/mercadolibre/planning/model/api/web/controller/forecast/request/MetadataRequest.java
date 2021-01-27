package com.mercadolibre.planning.model.api.web.controller.forecast.request;

import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionMetadata;
import lombok.Value;

import javax.validation.constraints.NotBlank;

@Value
public class MetadataRequest {

    @NotBlank
    private String key;

    @NotBlank
    private String value;

    public PlanningDistributionMetadata toPlanningDistributionMetadata() {
        return PlanningDistributionMetadata.builder()
                .key(key)
                .value(value)
                .build();
    }
}

