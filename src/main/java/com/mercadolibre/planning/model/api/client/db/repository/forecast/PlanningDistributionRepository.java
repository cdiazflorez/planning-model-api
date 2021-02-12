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
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface PlanningDistributionRepository extends CrudRepository<PlanningDistribution, Long> {

    @Query(value = "WITH forecast_current as ( "
            + "   SELECT max(f.id) as id "
            + "   FROM forecast_metadata fm "
            + "   INNER JOIN forecast_metadata fm2 ON fm.forecast_id = fm2.forecast_id "
            + "   INNER JOIN forecast f ON f.id = fm.forecast_id "
            + "   WHERE (fm.`key` = 'warehouse_id' AND fm.value = :warehouse_id) "
            + "   AND (fm2.`key` = 'week' AND fm2.value in :weeks) "
            + "   AND f.workflow = :workflow "
            + "   GROUP BY fm2.value, fm.value, f.workflow ) "
            + "SELECT date_in as dateIn, date_out as dateOut, "
            + "   round(quantity + COALESCE(quantity * cfd.value, 0)) as quantity "
            + "FROM forecast_current fc, "
            + "   planning_distribution p "
            + "LEFT JOIN current_forecast_deviation cfd ON "
            + "   :apply_deviation = true "
            + "   AND cfd.logistic_center_id = :warehouse_id "
            + "   AND cfd.is_active = true "
            + "   AND p.date_in BETWEEN cfd.date_from AND cfd.date_to "
            + "WHERE p.date_out BETWEEN :date_out_from AND :date_out_to "
            + "   AND p.forecast_id in (fc.id)", nativeQuery = true)
    List<PlanningDistributionView> findByWarehouseIdWorkflowAndDateOutInRange(
            @Param("warehouse_id") String warehouseId,
            @Param("workflow") String workflow,
            @Param("date_out_from") ZonedDateTime dateOutFrom,
            @Param("date_out_to") ZonedDateTime dateOutTo,
            @Param("weeks") Set<String> weeks,
            @Param("apply_deviation") boolean applyDeviation);

    @Query(value = "WITH forecast_current as ("
            + "   SELECT max(f.id) as id"
            + "   from forecast_metadata fm"
            + "   INNER JOIN forecast_metadata fm2 on fm.forecast_id = fm2.forecast_id"
            + "   INNER JOIN forecast f on f.id = fm.forecast_id"
            + "   WHERE (fm.`key` = 'warehouse_id' and fm.value = :warehouse_id)"
            + "   AND (fm2.`key` = 'week' and fm2.value in :weeks)"
            + "   AND f.workflow = :workflow "
            + "   GROUP BY fm2.value, fm.value, f.workflow ) "
            + "SELECT date_in as dateIn, date_out as dateOut, "
            + "   round(quantity + COALESCE(quantity * cfd.value, 0)) as quantity "
            + "FROM forecast_current fc, planning_distribution p "
            + "LEFT JOIN current_forecast_deviation cfd ON "
            + "   :apply_deviation = true "
            + "   AND cfd.logistic_center_id = :warehouse_id "
            + "   AND cfd.is_active = true "
            + "   AND p.date_in BETWEEN cfd.date_from AND cfd.date_to "
            + "WHERE p.date_out BETWEEN :date_out_from AND :date_out_to "
            + "   AND p.date_in BETWEEN :date_in_from AND :date_in_to "
            + "   AND p.forecast_id in (fc.id)", nativeQuery = true)
    List<PlanningDistributionView> findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
            @Param("warehouse_id") String warehouseId,
            @Param("workflow") String workflow,
            @Param("date_out_from") ZonedDateTime dateOutFrom,
            @Param("date_out_to") ZonedDateTime dateOutTo,
            @Param("date_in_from") ZonedDateTime dateInFrom,
            @Param("date_in_to") ZonedDateTime dateInTo,
            @Param("weeks") Set<String> weeks,
            @Param("apply_deviation") boolean applyDeviation);

    @Query(value = "WITH forecast_current as ( "
            + "   SELECT max(f.id) as id "
            + "   from forecast_metadata fm "
            + "   INNER JOIN forecast_metadata fm2 on fm.forecast_id = fm2.forecast_id "
            + "   INNER JOIN forecast f on f.id = fm.forecast_id "
            + "   WHERE (fm.`key` = 'warehouse_id' and fm.value = :warehouse_id) "
            + "   AND (fm2.`key` = 'week' and fm2.value in :weeks) "
            + "   AND f.workflow = :workflow "
            + "   GROUP BY fm2.value, fm.value, f.workflow ) "
            + "SELECT date_in as dateIn, date_out as dateOut, "
            + "   round(quantity + COALESCE(quantity * cfd.value, 0)) as quantity "
            + "FROM forecast_current fc, planning_distribution p "
            + "LEFT JOIN current_forecast_deviation cfd ON "
            + "   :apply_deviation = true "
            + "   AND cfd.logistic_center_id = :warehouse_id "
            + "   AND cfd.is_active = true "
            + "   AND p.date_in BETWEEN cfd.date_from AND cfd.date_to "
            + "WHERE p.date_out BETWEEN :date_out_from AND :date_out_to "
            + "   AND p.date_in <= :date_in_to "
            + "   AND p.forecast_id in (fc.id)", nativeQuery = true)
    List<PlanningDistributionView> findByWarehouseIdWorkflowAndDateOutInRangeAndDateInLessThan(
            @Param("warehouse_id") String warehouseId,
            @Param("workflow") String workflow,
            @Param("date_out_from") ZonedDateTime dateOutFrom,
            @Param("date_out_to") ZonedDateTime dateOutTo,
            @Param("date_in_to") ZonedDateTime dateInTo,
            @Param("weeks") Set<String> weeks,
            @Param("apply_deviation") boolean applyDeviation);

    @Query(value = "WITH forecast_current as ( "
            + "   SELECT max(f.id) as id "
            + "   from forecast_metadata fm "
            + "   INNER JOIN forecast_metadata fm2 on fm.forecast_id = fm2.forecast_id "
            + "   INNER JOIN forecast f on f.id = fm.forecast_id "
            + "   WHERE (fm.`key` = 'warehouse_id' and fm.value = :warehouse_id) "
            + "   AND (fm2.`key` = 'week' and fm2.value in :weeks) "
            + "   AND f.workflow = :workflow "
            + "   GROUP BY fm2.value, fm.value, f.workflow ) "
            + "SELECT sum(ROUND(quantity + COALESCE(quantity * cfd.value, 0))) as quantity "
            + "FROM forecast_current fc, planning_distribution p "
            + "LEFT JOIN current_forecast_deviation cfd ON "
            + "   :apply_deviation = true "
            + "   AND cfd.logistic_center_id = :warehouse_id "
            + "   AND cfd.is_active = true "
            + "   AND p.date_in BETWEEN cfd.date_from AND cfd.date_to "
            + "WHERE p.date_in BETWEEN :date_in_from AND :date_in_to "
            + "AND p.forecast_id in (fc.id)", nativeQuery = true)
    SuggestedWavePlanningDistributionView findByWarehouseIdWorkflowDateInRange(
            @Param("warehouse_id") String warehouseId,
            @Param("workflow") String workflow,
            @Param("date_in_from") ZonedDateTime dateInFrom,
            @Param("date_in_to") ZonedDateTime dateInTo,
            @Param("weeks") Set<String> weeks,
            @Param("apply_deviation") boolean applyDeviation);
}
