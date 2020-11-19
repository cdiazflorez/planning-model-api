package com.mercadolibre.planning.model.api.client.db.repository.current;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentHeadcountProductivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface CurrentHeadcountProductivityRepository
        extends JpaRepository<CurrentHeadcountProductivity, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE "
            + "  CurrentHeadcountProductivity chp "
            + "SET "
            + "   chp.isActive = false "
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
            @Param("ability_level") long abilityLevel);

    @Query("SELECT "
            + " cpd "
            + "FROM CurrentHeadcountProductivity cpd "
            + "WHERE "
            + "   cpd.processName IN (:process_name) "
            + "   AND cpd.date BETWEEN :date_from AND :date_to "
            + "   AND cpd.workflow = :workflow "
            + "   AND cpd.logisticCenterId = :warehouse_id "
            + "   AND cpd.isActive = true "
            + "ORDER BY cpd.date")
    List<CurrentHeadcountProductivity>
            findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
            @Param("warehouse_id") String warehouseId,
            @Param("workflow") Workflow workflow,
            @Param("process_name") List<ProcessName> processNames,
            @Param("date_from") ZonedDateTime dateFrom,
            @Param("date_to") ZonedDateTime dateTo);
}
