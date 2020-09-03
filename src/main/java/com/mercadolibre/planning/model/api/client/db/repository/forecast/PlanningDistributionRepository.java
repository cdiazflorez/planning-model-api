package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanningDistributionRepository
        extends CrudRepository<PlanningDistribution, Long> {
}
