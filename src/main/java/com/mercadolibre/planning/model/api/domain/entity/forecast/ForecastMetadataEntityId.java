package com.mercadolibre.planning.model.api.domain.entity.forecast;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
public class ForecastMetadataEntityId implements Serializable {

    private static final long serialVersionUID = -8778186008039745895L;

    private long forecastId;
    private String key;
}
