package com.mercadolibre.planning.model.api.web.controller.configuration;

import lombok.Value;

@Value
public class ConfigurationResponse {

    private long value;

    private String metricUnit;
}
