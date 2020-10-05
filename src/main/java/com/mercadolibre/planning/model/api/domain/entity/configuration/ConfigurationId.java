package com.mercadolibre.planning.model.api.domain.entity.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationId implements Serializable {

    private static final long serialVersionUID = -8778186008039745895L;

    private String logisticCenterId;

    private String key;
}
