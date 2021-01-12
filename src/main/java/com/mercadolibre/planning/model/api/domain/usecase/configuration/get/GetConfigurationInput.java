package com.mercadolibre.planning.model.api.domain.usecase.configuration.get;

import lombok.Value;

@Value
public class GetConfigurationInput {

    private String logisticCenterId;

    private String key;
}
