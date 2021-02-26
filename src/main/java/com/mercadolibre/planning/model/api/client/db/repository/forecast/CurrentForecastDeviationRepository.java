package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CurrentForecastDeviationRepository
        extends CrudRepository<CurrentForecastDeviation, Long> {
    List<CurrentForecastDeviation> findByLogisticCenterId(final String logisticCenterId);

    Optional<CurrentForecastDeviation>
            findByLogisticCenterIdAndWorkflowAndIsActiveTrueAndDateToIsGreaterThanEqual(
                    final String logisticCenterId,
                    final Workflow workflow,
                    final ZonedDateTime dateTo);
}
