package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface PlanningDistributionRepository extends CrudRepository<PlanningDistribution, Long> {

    @Query("SELECT "
            + " p "
            + "FROM PlanningDistribution p "
            + "WHERE "
            + "   p.dateOut BETWEEN :date_out_from AND :date_out_to "
            + "   AND p.forecast.id = "
            + "   ("
            + "      SELECT "
            + "         max(f.id) "
            + "      FROM "
            + "         Forecast f, "
            + "         ForecastMetadata fm "
            + "      WHERE "
            + "         f.workflow = :workflow "
            + "         and fm.forecastId = f.id "
            + "         and fm.key = 'warehouse_id' "
            + "         and fm.value = :warehouse_id"
            + "   )")
    List<PlanningDistribution> findByWarehouseIdWorkflowAndDateOutInRange(
            @Param("warehouse_id") String warehouseId,
            @Param("workflow") Workflow workflow,
            @Param("date_out_from") ZonedDateTime dateOutFrom,
            @Param("date_out_to") ZonedDateTime dateOutTo);
}
