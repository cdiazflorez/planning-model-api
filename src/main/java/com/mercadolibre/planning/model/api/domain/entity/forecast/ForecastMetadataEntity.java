package com.mercadolibre.planning.model.api.domain.entity.forecast;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

@Entity
@IdClass(ForecastMetadataEntityId.class)
@Getter
@Builder
@AllArgsConstructor
@Table(name = "forecast_metadata")
public class ForecastMetadataEntity {

    @Id
    @JoinColumn(name = "forecast_id")
    private long forecastId;

    @Id
    @Column(name = "`key`")
    private String key;

    @Column(name = "`value`")
    private String value;
}
