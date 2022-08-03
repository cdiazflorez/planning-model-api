package com.mercadolibre.planning.model.api.client.db.repository.inputoptimization;

import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.InputOptimizationService.InputOptimizationRepository;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InputOptimizationRepositoryImpl implements InputOptimizationRepository {

    private final InputOptimizationJpaRepository inputOptimizationJpaRepository;

    @Override
    public Map<DomainType, String> getInputs(final String warehouseId,
                                             final Set<DomainType> domains) {

        if (domains.isEmpty()) {
            return inputOptimizationJpaRepository.findAllByWarehouseId(warehouseId).stream()
                    .collect(Collectors.toMap(
                            InputOptimizationView::getDomain,
                            InputOptimizationView::getJsonValue)
                    );
        } else {
            return inputOptimizationJpaRepository.findAllByWarehouseIdAndDomainIn(warehouseId, domains).stream()
                    .collect(Collectors.toMap(
                            InputOptimizationView::getDomain,
                            InputOptimizationView::getJsonValue)
                    );
        }
    }
}
