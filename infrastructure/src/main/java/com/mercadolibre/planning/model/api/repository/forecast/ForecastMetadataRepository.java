package com.mercadolibre.planning.model.api.repository.forecast;

import com.mercadolibre.planning.model.api.dao.forecast.ForecastMetadataDao;
import com.mercadolibre.planning.model.api.dao.forecast.ForecastMetadataDaoId;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForecastMetadataRepository
        extends CrudRepository<ForecastMetadataDao, ForecastMetadataDaoId> {
}
