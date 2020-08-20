package com.mercadolibre.planning.model.api.dao.forecast;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
public class ForecastMetadataDaoId implements Serializable {

    private static final long serialVersionUID = -8778186008039745895L;

    private long forecastId;
    private String key;
}
