package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface ProcessingDistributionRepository
        extends CrudRepository<ProcessingDistribution, Long> {

    @Query("SELECT "
            + " p "
            + "FROM "
            + "   ProcessingDistribution p "
            + "WHERE "
            + "   p.date >= :date_from "
            + "   AND p.date <= :date_to "
            + "   AND p.forecast.id = "
            + "   ("
            + "      SELECT "
            + "         MAX(f.id) "
            + "      FROM "
            + "         Forecast f, "
            + "         ForecastMetadata m "
            + "      WHERE "
            + "         f.workflow = :workflow "
            + "         AND m.forecastId = f.id "
            + "         AND m.key = 'warehouse_id' "
            + "         AND m.value = :warehouse_id"
            + "   ) "
            + "   AND p.type = :type")
    List<ProcessingDistribution> findByWarehouseIdAndWorkflowAndTypeAndDateInRange(
            @Param("warehouse_id") String warehouseId,
            @Param("workflow") Workflow workflow,
            @Param("type") ProcessingType type,
            @Param("date_from") ZonedDateTime dateFrom,
            @Param("date_to") ZonedDateTime dateTo);

}
