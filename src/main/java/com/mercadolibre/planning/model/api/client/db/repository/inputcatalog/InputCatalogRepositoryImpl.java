package com.mercadolibre.planning.model.api.client.db.repository.inputcatalog;

import com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId;
import com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.InputCatalogService.InputCatalogRepository;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InputCatalogRepositoryImpl implements InputCatalogRepository {

    private final InputOptimizationJpaRepository inputOptimizationJpaRepository;

    @Override
    public Map<InputId, String> getInputs(final String warehouseId,
                                          final Set<InputId> domains) {

        if (domains.isEmpty()) {
            return inputOptimizationJpaRepository.findAllByWarehouseId(warehouseId).stream()
                    .collect(Collectors.toMap(
                            InputCatalogView::getDomain,
                            InputCatalogView::getJsonValue)
                    );
        } else {
            return inputOptimizationJpaRepository.findAllByWarehouseIdAndDomainIn(warehouseId, domains).stream()
                    .collect(Collectors.toMap(
                            InputCatalogView::getDomain,
                            InputCatalogView::getJsonValue)
                    );
        }
    }
}
