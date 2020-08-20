package com.mercadolibre.planning.model.api.repository.forecast;

import com.mercadolibre.planning.model.api.dao.forecast.PlanningDistributionMetadataDao;
import com.mercadolibre.planning.model.api.dao.forecast.PlanningDistributionMetadataDaoId;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanningMetadataRepository
        extends CrudRepository<PlanningDistributionMetadataDao, PlanningDistributionMetadataDaoId> {
}
