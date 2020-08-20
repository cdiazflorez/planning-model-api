package com.mercadolibre.planning.model.api.repository.current;

import com.mercadolibre.planning.model.api.dao.current.CurrentHeadcountProductivityDao;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrentHeadcountProductivityRepository
        extends CrudRepository<CurrentHeadcountProductivityDao, Long> {
}
