package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface PlanningDistributionRepository extends CrudRepository<PlanningDistribution, Long> {

    @Query(value = "SELECT * "
            + "FROM planning_distribution p "
            + "WHERE p.date_out BETWEEN :date_out_from AND :date_out_to "
            + "AND p.forecast_id in ("
            + " SELECT MAX(fm.id) FROM "
            + "     (SELECT id, "
            + "     workflow, "
            + "     (SELECT m.value FROM forecast_metadata m "
            + "     WHERE m.forecast_id = f.id AND m.key = 'warehouse_id') AS warehouse_id, "
            + "     (SELECT m.value FROM forecast_metadata m "
            + "     WHERE m.forecast_id = f.id AND m.key = 'week') AS forecast_week "
            + "     FROM forecast f) fm "
            + "     WHERE fm.warehouse_id = :warehouse_id "
            + "     AND fm.workflow = :workflow "
            + "     AND fm.forecast_week in (:weeks)"
            + "     GROUP BY fm.forecast_week)", nativeQuery = true)
    List<PlanningDistributionView> findByWarehouseIdWorkflowAndDateOutInRange(
            @Param("warehouse_id") String warehouseId,
            @Param("workflow") String workflow,
            @Param("date_out_from") ZonedDateTime dateOutFrom,
            @Param("date_out_to") ZonedDateTime dateOutTo,
            @Param("weeks") Set<String> weeks);
}
