package com.mercadolibre.planning.model.api.client.db.repository.current;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CurrentProcessingDistributionRepository extends JpaRepository<CurrentProcessingDistribution, Long> {

  @Query("SELECT "
      + " cpd "
      + "FROM CurrentProcessingDistribution cpd "
      + "WHERE "
      + "   cpd.processName IN (:process_name) "
      + "   AND cpd.date BETWEEN :date_from AND :date_to "
      + "   AND cpd.workflow = :workflow "
      + "   AND cpd.type IN (:type) "
      + "   AND cpd.logisticCenterId = :warehouse_id "
      + "   AND cpd.isActive = true "
      + "ORDER BY cpd.date")
  List<CurrentProcessingDistribution>
  findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
      @Param("warehouse_id") String warehouseId,
      @Param("workflow") Workflow workflow,
      @Param("type") Set<ProcessingType> type,
      @Param("process_name") List<ProcessName> processNames,
      @Param("date_from") ZonedDateTime dateFrom,
      @Param("date_to") ZonedDateTime dateTo);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Transactional
  @Query("UPDATE "
      + " CurrentProcessingDistribution cpd "
      + "SET "
      + "   cpd.isActive = false, "
      + "   cpd.userId = :user_id,"
      + "   cpd.lastUpdated = CURRENT_TIMESTAMP "
      + "WHERE "
      + "   cpd.logisticCenterId = :logistic_center_id "
      + "   AND cpd.workflow = :workflow "
      + "   AND cpd.date BETWEEN :date_from AND :date_to"
      + "   AND cpd.isActive = true")
  void deactivateProcessingDistributionForRangeOfDates(
      @Param("logistic_center_id") String logisticCenterId,
      @Param("workflow") Workflow workflow,
      @Param("date_from") ZonedDateTime dateFrom,
      @Param("date_to") ZonedDateTime dateTo,
      @Param("user_id") long userId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Transactional
  @Query("UPDATE "
      + " CurrentProcessingDistribution cpd "
      + "SET "
      + "   cpd.isActive = false, "
      + "   cpd.userId = :user_id,"
      + "   cpd.lastUpdated = CURRENT_TIMESTAMP "
      + "WHERE "
      + "   cpd.processName = :process_name"
      + "   AND cpd.workflow = :workflow "
      + "   AND cpd.logisticCenterId = :logistic_center_id "
      + "   AND cpd.quantityMetricUnit = :metric_unit "
      + "   AND cpd.type = :type "
      + "   AND cpd.date in :dates"
      + "   AND cpd.isActive = true")
  void deactivateProcessingDistribution(
      @Param("logistic_center_id") String logisticCenterId,
      @Param("workflow") Workflow workflow,
      @Param("process_name") ProcessName processName,
      @Param("dates") List<ZonedDateTime> dates,
      @Param("type") ProcessingType type,
      @Param("user_id") long userId,
      @Param("metric_unit") MetricUnit metricUnit);

  @Query(
      value = "SELECT cpd.* "
          + "FROM current_processing_distribution cpd "
          + "WHERE cpd.logistic_center_id = :warehouse_id "
          + "    AND cpd.workflow = :workflow "
          + "    AND cpd.process_path IN (:process_paths) "
          + "    AND cpd.process_name IN (:process_name) "
          + "    AND cpd.`type` IN (:type) "
          + "    AND cpd.date BETWEEN :date_from AND :date_to "
          + "    AND date_created <= :view_date "
          + "    AND (last_updated = date_created OR :view_date < last_updated) "
          + "ORDER BY date",
      nativeQuery = true)
  List<CurrentProcessingDistribution> findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRangeAtViewDate(
      @Param("warehouse_id") String warehouseId,
      @Param("workflow") String workflow,
      @Param("process_paths") Set<String> processPaths,
      @Param("process_name") Set<String> processNames,
      @Param("type") Set<String> type,
      @Param("date_from") ZonedDateTime dateFrom,
      @Param("date_to") ZonedDateTime dateTo,
      @Param("view_date") Instant viewDate
  );

  @Query("""
      SELECT cpd.type AS type, MAX(cpd.dateCreated) AS dateCreated 
         FROM  CurrentProcessingDistribution cpd  
       WHERE cpd.logisticCenterId  = :warehouse_id 
         AND cpd.workflow = :workflow 
         AND cpd.type IN (:type) 
         AND cpd.isActive = true 
       AND cpd.dateCreated >= :date_time
        GROUP BY cpd.type
        """)
  List<CurrentProcessingMaxDateCreatedByType> findDateCreatedByWarehouseIdAndWorkflowAndTypeAndIsActive(
      @Param("warehouse_id") String warehouseId,
      @Param("workflow") Workflow workflow,
      @Param("type") Set<ProcessingType> type,
      @Param("date_time") ZonedDateTime dateTime
  );

}
