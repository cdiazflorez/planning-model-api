package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurrentForecastDeviationRepository
        extends CrudRepository<CurrentForecastDeviation, Long> {

    Optional<CurrentForecastDeviation> findBylogisticCenterIdAndWorkflowAndIsActive(
            final String warehouseId, final Workflow workflow, final boolean isActive);
}
