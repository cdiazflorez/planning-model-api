package com.mercadolibre.planning.model.api.repository.forecast;

import com.mercadolibre.planning.model.api.dao.forecast.HeadcountProductivityDao;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HeadcountProductivityRepository
        extends CrudRepository<HeadcountProductivityDao, Long> {
}
