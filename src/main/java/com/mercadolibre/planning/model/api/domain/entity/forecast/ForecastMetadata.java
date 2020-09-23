package com.mercadolibre.planning.model.api.domain.entity.forecast;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;

@Entity
@IdClass(ForecastMetadataId.class)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ForecastMetadata {

    @Id
    @JoinColumn(name = "forecast_id")
    private long forecastId;

    @Id
    @Column(name = "`key`")
    private String key;

    @Column(name = "`value`")
    private String value;
}
