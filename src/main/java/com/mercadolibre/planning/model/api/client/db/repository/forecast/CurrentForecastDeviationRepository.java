package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CurrentForecastDeviationRepository
        extends CrudRepository<CurrentForecastDeviation, Long> {
    List<CurrentForecastDeviation> findByLogisticCenterId(final String logisticCenterId);


    @Query(value = "SELECT * FROM current_forecast_deviation cfd "
            + "WHERE cfd.logistic_center_id = :logistic_center_id "
            + "AND cfd.workflow = :workflow "
            + "AND cfd.is_active = :is_active "
            + "AND :current_date BETWEEN cfd.date_from AND cfd.date_to;", nativeQuery = true)
    Optional<CurrentForecastDeviation> findByLogisticCenterIdAndWorkflowAndIsActiveAndDateInRange(
            @Param("logistic_center_id") final String logisticCenterId,
            @Param("workflow") final String workflow,
            @Param("is_active") final boolean isActive,
            @Param("current_date") final ZonedDateTime currentDate);
}
