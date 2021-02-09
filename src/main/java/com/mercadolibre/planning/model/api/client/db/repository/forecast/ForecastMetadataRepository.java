package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadataId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForecastMetadataRepository
        extends CrudRepository<ForecastMetadata, ForecastMetadataId> {

    @Query(value = "SELECT forecast_metadata.key, forecast_metadata.value"
            + "    FROM forecast_metadata "
            + "    WHERE forecast_metadata.key in (:metadata_keys)"
            + "    AND forecast_metadata.forecast_id in (:forecast_ids)", nativeQuery = true)
    List<ForecastMetadataView> findLastForecastMetadataByWarehouseId(
            @Param("metadata_keys") List<String> cardinalityDist,
            @Param("forecast_ids") List<Long> forecastIds
    );
}
