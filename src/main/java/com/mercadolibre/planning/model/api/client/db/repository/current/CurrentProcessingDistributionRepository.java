package com.mercadolibre.planning.model.api.client.db.repository.current;

import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrentProcessingDistributionRepository
        extends CrudRepository<CurrentProcessingDistribution, Long> {
}
