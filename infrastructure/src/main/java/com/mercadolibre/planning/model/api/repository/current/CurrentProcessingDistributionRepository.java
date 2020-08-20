package com.mercadolibre.planning.model.api.repository.current;

import com.mercadolibre.planning.model.api.dao.current.CurrentProcessingDistributionDao;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrentProcessingDistributionRepository
        extends CrudRepository<CurrentProcessingDistributionDao, Long> {
}
