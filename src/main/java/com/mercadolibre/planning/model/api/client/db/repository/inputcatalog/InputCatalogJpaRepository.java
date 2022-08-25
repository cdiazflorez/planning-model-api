package com.mercadolibre.planning.model.api.client.db.repository.inputcatalog;

import com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId;
import com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputCatalog;
import com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputCatalogId;
import java.util.Set;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InputCatalogJpaRepository extends CrudRepository<InputCatalog, InputCatalogId> {

    Set<InputCatalogView> findAllByWarehouseIdAndDomainIn(String warehouseId, Set<InputId> inputIds);

    Set<InputCatalogView> findAllByWarehouseId(String warehouseId);

}
