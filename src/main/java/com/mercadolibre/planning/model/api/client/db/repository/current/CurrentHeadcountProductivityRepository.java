package com.mercadolibre.planning.model.api.client.db.repository.current;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentHeadcountProductivity;
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
public interface CurrentHeadcountProductivityRepository extends JpaRepository<CurrentHeadcountProductivity, Long> {

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Transactional
  @Query("UPDATE "
      + "  CurrentHeadcountProductivity chp "
      + "SET "
      + "   chp.isActive = false, "
      + "   chp.userId = :user_id, "
      + "   chp.lastUpdated = CURRENT_TIMESTAMP "
      + "WHERE  "
      + "   chp.processName = :process_name "
      + "   AND chp.workflow = :workflow "
      + "   AND chp.logisticCenterId = :logistic_center_id "
      + "   AND chp.productivityMetricUnit = :metric_unit "
      + "   AND chp.abilityLevel = :ability_level   "
      + "   AND chp.isActive = true "
      + "   AND chp.date in :dates")
  void deactivateProductivity(
      @Param("logistic_center_id") String logisticCenterId,
      @Param("workflow") Workflow workflow,
      @Param("process_name") ProcessName processName,
      @Param("dates") List<ZonedDateTime> dates,
      @Param("metric_unit") MetricUnit metricUnit,
      @Param("user_id") long userId,
      @Param("ability_level") int abilityLevel
  );

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Transactional
  @Query("UPDATE "
      + " CurrentHeadcountProductivity chp "
      + " SET "
      + " chp.isActive = false, "
      + " chp.userId = :user_id, "
      + " chp.lastUpdated = CURRENT_TIMESTAMP "
      + " WHERE "
      + " chp.logisticCenterId = :logistic_center_id "
      + " AND chp.workflow = :workflow "
      + " AND chp.date BETWEEN :date_from AND :date_to"
      + " AND chp.isActive = true ")
  void deactivateProductivityForRangeOfDates(
      @Param("logistic_center_id") String logisticCenterId,
      @Param("workflow") Workflow workflow,
      @Param("date_from") ZonedDateTime dateFrom,
      @Param("date_to") ZonedDateTime dateTo,
      @Param("user_id") long userId
  );

  @Query(
      value = "SELECT cpd.* "
          + "FROM current_headcount_productivity cpd "
          + "WHERE cpd.process_name IN (:process_name) "
          + "    AND cpd.date BETWEEN :date_from AND :date_to "
          + "    AND cpd.workflow = :workflow "
          + "    AND cpd.logistic_center_id = :warehouse_id "
          + "    AND date_created <= :view_date "
          + "    AND (last_updated = date_created OR :view_date < last_updated) "
          + "ORDER BY date",
      nativeQuery = true)
  List<CurrentHeadcountProductivity> findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRangeAtViewDate(
      @Param("warehouse_id") String warehouseId,
      @Param("workflow") String workflow,
      @Param("process_name") Set<String> processNames,
      @Param("date_from") ZonedDateTime dateFrom,
      @Param("date_to") ZonedDateTime dateTo,
      @Param("view_date") Instant viewDate
  );
}
