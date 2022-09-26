package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CurrentForecastDeviationRepository extends CrudRepository<CurrentForecastDeviation, Long> {

  List<CurrentForecastDeviation> findByLogisticCenterIdAndWorkflowAndIsActiveTrue(String logisticCenterId, Workflow workflow);

  Optional<CurrentForecastDeviation> findByLogisticCenterIdAndWorkflowAndIsActiveTrueAndDateToIsGreaterThanEqual(
      String logisticCenterId,
      Workflow workflow,
      ZonedDateTime dateTo
  );

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Transactional
  @Query("UPDATE "
      + "  CurrentForecastDeviation cfd "
      + "SET "
      + "   cfd.isActive = false "
      + "WHERE  "
      + "   cfd.workflow =:workflow "
      + "   AND cfd.logisticCenterId =:logistic_center_id "
      + "   AND cfd.isActive = true ")
  void disableDeviation(
      @Param("logistic_center_id") String logisticCenterId,
      @Param("workflow") Workflow workflow);

  @Query(value =
      "SELECT * "
          + "FROM current_forecast_deviation "
          + "WHERE id IN ( "
          + "    SELECT MAX(id) as id"
          + "    FROM current_forecast_deviation cfd "
          + "    WHERE cfd.logistic_center_id = :logistic_center_id "
          + "        AND cfd.workflow = :workflow "
          + "        AND cfd.date_created <= :view_date "
          + "        AND (:view_date < cfd.last_updated OR cfd.last_updated = cfd.date_created)"
          + ")",
      nativeQuery = true)
  List<CurrentForecastDeviation> findActiveDeviationAt(
      @Param("logistic_center_id") String logisticCenterId,
      @Param("workflow") String workflow,
      @Param("view_date") Instant viewDate
  );
}
