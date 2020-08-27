package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionMetadataEntity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionMetadataEntityId;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanningMetadataRepository
        extends CrudRepository<PlanningDistributionMetadataEntity,
        PlanningDistributionMetadataEntityId> {
}
