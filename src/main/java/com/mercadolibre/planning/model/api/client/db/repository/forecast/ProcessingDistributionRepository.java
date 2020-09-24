package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
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
            + "   p.processName IN "
            + "   ( "
            + "      :process_name"
            + "   ) "
            + "   AND p.date BETWEEN :date_from AND :date_to "
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
            + "   AND p.type = :type "
            + "ORDER BY p.date")
    List<ProcessingDistribution> findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
            @Param("warehouse_id") String warehouseId,
            @Param("workflow") Workflow workflow,
            @Param("type") ProcessingType type,
            @Param("process_name") List<ProcessName> processNames,
            @Param("date_from") ZonedDateTime dateFrom,
            @Param("date_to") ZonedDateTime dateTo);

}
