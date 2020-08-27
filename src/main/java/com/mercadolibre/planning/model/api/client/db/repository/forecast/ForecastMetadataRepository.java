package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadataEntity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadataEntityId;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForecastMetadataRepository
        extends CrudRepository<ForecastMetadataEntity, ForecastMetadataEntityId> {
}
