package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface ProcessingDistributionRepository
        extends CrudRepository<ProcessingDistribution, Long> {

    @Query(value = "SELECT p.date, process_name as processName, quantity,"
            + " quantity_metric_unit as quantityMetricUnit, type "
            + "FROM processing_distribution p "
            + "WHERE p.process_name IN (:process_name) "
            + "AND p.date BETWEEN :date_from AND :date_to "
            + "AND (COALESCE(:type) is NULL OR p.type IN (:type)) "
            + "AND p.forecast_id in ("
            + " SELECT MAX(id) FROM "
            + "     (SELECT id, "
            + "     workflow, "
            + "     (SELECT m.value FROM forecast_metadata m "
            + "     WHERE m.forecast_id = f.id AND m.key = 'warehouse_id') AS warehouse_id, "
            + "     (SELECT m.value FROM forecast_metadata m "
            + "     WHERE m.forecast_id = f.id AND m.key = 'week') AS forecast_week "
            + "     FROM forecast f) forecast_with_metadata "
            + "     WHERE warehouse_id = :warehouse_id "
            + "     AND workflow = :workflow "
            + "     AND forecast_week IN (:weeks) "
            + "     GROUP BY forecast_week)", nativeQuery = true)
    List<ProcessingDistributionView> findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
            @Param("warehouse_id") String warehouseId,
            @Param("workflow") String workflow,
            @Param("type") Set<String> type,
            @Param("process_name") List<String> processNames,
            @Param("date_from") ZonedDateTime dateFrom,
            @Param("date_to") ZonedDateTime dateTo,
            @Param("weeks") Set<String> weeks);
}
