package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CurrentForecastDeviationRepository
        extends CrudRepository<CurrentForecastDeviation, Long> {
    List<CurrentForecastDeviation> findByLogisticCenterId(final String logisticCenterId);

    List<CurrentForecastDeviation> findByLogisticCenterIdAndIsActiveTrue(final String logisticCenterId);

    Optional<CurrentForecastDeviation>
            findByLogisticCenterIdAndWorkflowAndIsActiveTrueAndDateToIsGreaterThanEqual(
                    final String logisticCenterId,
                    final Workflow workflow,
                    final ZonedDateTime dateTo);

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
}
