package com.mercadolibre.planning.model.api.repository.forecast;

import com.mercadolibre.planning.model.api.dao.forecast.HeadcountDistributionDao;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HeadcountDistributionRepository
        extends CrudRepository<HeadcountDistributionDao, Long> {
}
