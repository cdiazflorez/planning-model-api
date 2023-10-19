package com.mercadolibre.planning.model.api.domain.entity.configuration;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;

import java.time.ZonedDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ConfigurationId.class)
public class Configuration {

    @Id
    private String logisticCenterId;

    @Id
    @Column(name = "`key`", length = 80)
    private String key;

    @Column(name = "`value`")
    private String value;

    @Enumerated(EnumType.STRING)
    private MetricUnit metricUnit;

    @CreationTimestamp
    @Column(updatable = false)
    private ZonedDateTime dateCreated;

    @UpdateTimestamp
    private ZonedDateTime lastUpdated;

    private long lastUserUpdated;
}
