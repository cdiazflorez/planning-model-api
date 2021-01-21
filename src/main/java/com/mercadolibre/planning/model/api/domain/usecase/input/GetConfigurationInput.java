package com.mercadolibre.planning.model.api.domain.usecase.input;

import lombok.Value;

@Value
public class GetConfigurationInput {

    private String logisticCenterId;

    private String key;
}
