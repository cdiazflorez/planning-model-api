package com.mercadolibre.planning.model.api.domain.entity.configuration;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@IdClass(ConfigurationId.class)
public class Configuration {

    @Id
    private String logisticCenterId;

    @Id
    private String key;

    private long value;

    @Enumerated(EnumType.STRING)
    private MetricUnit metricUnit;
}
