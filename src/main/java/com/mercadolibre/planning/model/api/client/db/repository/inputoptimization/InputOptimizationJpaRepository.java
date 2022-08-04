package com.mercadolibre.planning.model.api.client.db.repository.inputoptimization;

import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType;
import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.InputOptimization;
import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.InputOptimizationId;
import java.util.Set;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InputOptimizationJpaRepository extends CrudRepository<InputOptimization, InputOptimizationId> {

    Set<InputOptimizationView> findAllByWarehouseIdAndDomainIn(String warehouseId,
                                                              Set<DomainType> domainTypes);

    Set<InputOptimizationView> findAllByWarehouseId(String warehouseId);

}
