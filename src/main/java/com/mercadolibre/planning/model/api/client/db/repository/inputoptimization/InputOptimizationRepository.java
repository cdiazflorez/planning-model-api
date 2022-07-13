package com.mercadolibre.planning.model.api.client.db.repository.inputoptimization;

import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType;
import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.InputOptimization;
import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.InputOptimizationId;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InputOptimizationRepository extends CrudRepository<InputOptimization, InputOptimizationId> {

    List<InputOptimizationView> findAllByWarehouseIdAndDomainIn(String warehouseId,
                                                               List<DomainType> domainTypes);

    List<InputOptimizationView> findAllByWarehouseId(String warehouseId);

}
