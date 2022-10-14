package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.adapter.headcount.ProcessPathShareAdapter.ShareView;
import com.mercadolibre.planning.model.api.domain.entity.forecast.MaxCapacityView;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessingDistributionRepository extends CrudRepository<ProcessingDistribution, Long> {

  @Query(value =
      "SELECT p.date, process_path as processPath, process_name as processName, quantity, quantity_metric_unit as quantityMetricUnit, type "
          + "FROM processing_distribution p "
          + "WHERE p.process_path IN (:process_name) "
          + "AND p.process_name IN (:process_name) "
          + "AND p.date BETWEEN :date_from AND :date_to "
          + "AND (COALESCE(:type) is NULL OR p.type IN (:type)) "
          + "AND p.forecast_id in (:forecast_ids)", nativeQuery = true)
  List<ProcessingDistributionView> findByWarehouseIdWorkflowTypeProcessPathProcessNameAndDateInRange(
      @Param("type") Set<String> type,
      @Param("process_paths") List<String> processPaths,
      @Param("process_name") List<String> processNames,
      @Param("date_from") ZonedDateTime dateFrom,
      @Param("date_to") ZonedDateTime dateTo,
      @Param("forecast_ids") List<Long> forecastIds);

  @Query(
      value =
          "SELECT m.value as logisticCenterId, "
              + "f.date_created as loadDate, p.date as maxCapacityDate, "
              + "p.quantity as maxCapacityValue "
              + "FROM processing_distribution p "
              + "JOIN forecast f ON f.id = p.forecast_id "
              + "JOIN forecast_metadata m ON m.forecast_id = p.forecast_id "
              + "WHERE p.type = 'MAX_CAPACITY' "
              + "AND f.workflow = :workflow "
              + "AND (m.value = :warehouse_id AND :workflow IS NULL "
              + "OR f.workflow = :workflow AND :warehouse_id IS NULL)"
              + "AND p.date BETWEEN :date_from AND :date_to "
              + "AND m.key = 'warehouse_id' "
              + "ORDER BY m.value, f.date_created, p.date",
      nativeQuery = true)
  List<MaxCapacityView> findMaxCapacitiesByDateInRange(
      @Param("warehouse_id") String warehouseId,
      @Param("workflow") String workflow,
      @Param("date_from") ZonedDateTime dateFrom,
      @Param("date_to") ZonedDateTime dateTo);

  @Query(value = "WITH totals AS ( "
      + "    SELECT pd.forecast_id, pd.process_name, pd.date, SUM(quantity) as quantity "
      + "    FROM processing_distribution pd "
      + "    WHERE pd.process_path = 'GLOBAL' "
      + "        AND pd.process_name IN (:process_names) "
      + "        AND pd.forecast_id IN (:forecast_ids) "
      + "        AND pd.type = 'ACTIVE_WORKERS' "
      + "        AND pd.date BETWEEN :date_from AND :date_to "
      + "    GROUP BY pd.forecast_id, pd.process_name, pd.date "
      + ") "
      + "SELECT pd.process_path as processPath, pd.process_name as processName, pd.date as date, (pd.quantity / tt.quantity) as share "
      + "FROM processing_distribution pd "
      + "INNER JOIN totals tt ON pd.forecast_id = tt.forecast_id AND pd.process_name = tt.process_name AND pd.date = tt.date "
      + "WHERE pd.forecast_id IN (:forecast_ids) "
      + "    AND pd.process_name IN (:process_names) "
      + "    AND pd.type = 'ACTIVE_WORKERS' "
      + "    AND pd.date BETWEEN :date_from AND :date_to "
      + "    AND NOT EXISTS( "
      + "        SELECT 1"
      + "        FROM totals t"
      + "        WHERE t.date = pd.date AND t.forecast_id > pd.forecast_id"
      + "    )",
      nativeQuery = true)
  List<ShareView> getProcessPathHeadcountShare(
      @Param("process_names") List<String> processNames,
      @Param("date_from") Instant dateFrom,
      @Param("date_to") Instant dateTo,
      @Param("forecast_ids") List<Long> forecastIds);

}
