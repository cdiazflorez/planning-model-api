package com.mercadolibre.planning.model.api.dao.forecast;

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
@IdClass(ForecastMetadataDaoId.class)
@Getter
@Builder
@AllArgsConstructor
@Table(name = "forecast_metadata")
public class ForecastMetadataDao {

    @Id
    @JoinColumn(name = "forecast_id")
    private long forecastId;

    @Id
    @Column(name = "`key`")
    private String key;

    @Column(name = "`value`")
    private String value;
}
