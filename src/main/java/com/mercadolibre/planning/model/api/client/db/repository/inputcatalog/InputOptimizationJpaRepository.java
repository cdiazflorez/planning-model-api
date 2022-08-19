package com.mercadolibre.planning.model.api.client.db.repository.inputcatalog;

import com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId;
import com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputOptimization;
import com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputOptimizationId;
import java.util.Set;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InputOptimizationJpaRepository extends CrudRepository<InputOptimization, InputOptimizationId> {

    Set<InputCatalogView> findAllByWarehouseIdAndDomainIn(String warehouseId,
                                                          Set<InputId> inputIds);

    Set<InputCatalogView> findAllByWarehouseId(String warehouseId);

}
