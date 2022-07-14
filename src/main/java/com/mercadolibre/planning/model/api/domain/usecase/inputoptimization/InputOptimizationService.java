package com.mercadolibre.planning.model.api.domain.usecase.inputoptimization;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.planning.model.api.client.db.repository.inputoptimization.InputOptimizationRepository;
import com.mercadolibre.planning.model.api.client.db.repository.inputoptimization.InputOptimizationView;
import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class InputOptimizationService {

    private ObjectMapper objectMapper;
    private InputOptimizationRepository inputOptimizationRepository;

    public Map<DomainType, Object> getInputOptimization(final InputOptimizationRequest request) {

        if (request.getDomains() == null || request.getDomains().isEmpty()) {
            return buildMapResponse(inputOptimizationRepository.findAllByWarehouseId(request.getWarehouseId()),
                                                                                    ofNullable(request.getDomainFilters()));
        } else {
            return buildMapResponse(inputOptimizationRepository.findAllByWarehouseIdAndDomainIn(request.getWarehouseId(),
                                                                                               request.getDomains()),
                                                                                               ofNullable(request.getDomainFilters()));
        }
    }

    private Map<DomainType, Object> buildMapResponse(final List<InputOptimizationView> inputOptimizationViews,
                                                     final Optional<Map<DomainType, Map<String, List<Object>>>> domainFilters) {
        return inputOptimizationViews.stream()
                .collect(toMap(
                        InputOptimizationView::getDomain,
                        input -> {
                            final Map<String, List<Object>> domainFilter = domainFilters.isPresent()
                                    ? domainFilters.get().getOrDefault(input.getDomain(), Map.of())
                                    : Map.of();
                            return getDomainValue(input.getDomain(), input.getJsonValue(), domainFilter);
                        }
                ));
    }

    private Object getDomainValue(final DomainType domainType,
                                  final String jsonValue,
                                  final Map<String, List<Object>> domainFilters) {
        try {
            DomainStrategy domainStrategy = domainType.domainStrategy;
            return domainStrategy.transformJsonValueToObject(objectMapper, jsonValue, domainFilters);
        } catch (JsonProcessingException e) {
            log.error(
                    String.format("Invalid Domain '%s' transform. Exceptions: %s", domainType.toJson(), e.getMessage())
            );
            return emptyList();
        }
    }

}
