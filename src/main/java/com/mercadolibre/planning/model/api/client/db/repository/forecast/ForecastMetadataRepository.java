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

    @Query(value = "SELECT key, value"
            + "    FROM forecast_metadata "
            + "    WHERE forecast_metadata.forecast_id = ( "
            + "        SELECT max(forecast_metadata.forecast_id) "
            + "        FROM forecast_metadata "
            + "        WHERE forecast_metadata.key = 'warehouse_id' "
            + "        AND forecast_metadata.value = :warehouse_id )", nativeQuery = true)
    List<ForecastMetadataView> findLastForecastMetadataByWarehouseId(
            @Param("warehouse_id") String warehouseId
    );
}
