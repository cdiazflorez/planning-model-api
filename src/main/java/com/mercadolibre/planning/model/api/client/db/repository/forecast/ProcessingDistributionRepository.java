package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.MaxCapacityView;
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

    @Query(value = "WITH distributions AS ( "
            + "SELECT p.date, process_name, quantity,"
            + " quantity_metric_unit, type, forecast_id "
            + "FROM processing_distribution p "
            + "WHERE p.process_name IN (:process_name) "
            + "AND p.date BETWEEN :date_from AND :date_to "
            + "AND (COALESCE(:type) is NULL OR p.type IN (:type)) "
            + "AND p.forecast_id in (:forecast_ids)"
            + ") " +
            "SELECT date, process_name as processName, quantity, quantity_metric_unit as quantityMetricUnit, type " +
            "FROM distributions d " +
            "WHERE NOT EXISTS (" +
            " SELECT 1 " +
            " FROM distributions d2" +
            " WHERE d.date = d2.date AND d.type = d2.type AND d.process_name = d2.process_name AND d.forecast_id < d2.forecast_id " +
            ")", nativeQuery = true)
    List<ProcessingDistributionView> findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
            @Param("type") Set<String> type,
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
}
