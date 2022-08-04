package com.mercadolibre.planning.model.api.domain.usecase.inputoptimization;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.get.GetInputOptimization;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.inputdomain.DomainStrategy;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class InputOptimizationService {

    private ObjectMapper objectMapper;

    private InputOptimizationRepository inputOptimizationRepository;

    public Map<DomainType, Object> getInputOptimization(final GetInputOptimization getInputOptimization) {

        return buildMapResponse(
                inputOptimizationRepository.getInputs(getInputOptimization.getWarehouseId(), getInputOptimization.getDomains().keySet()),
                getInputOptimization.getDomains()
        );

    }

    private Map<DomainType, Object> buildMapResponse(final Map<DomainType, String> domains,
                                                     final Map<DomainType, Map<String, List<Object>>> domainFilters) {

        return domains.entrySet().stream()
                .collect(toMap(
                        Entry::getKey,
                        domain -> getDomainValue(domain.getKey(), domain.getValue(), domainFilters.getOrDefault(domain.getKey(), Map.of()))
                ));
    }

    private Object getDomainValue(final DomainType domainType,
                                  final String jsonValue,
                                  final Map<String, List<Object>> domainFilters) {
        try {
            DomainStrategy domainStrategy = domainType.domainStrategy;
            return domainStrategy.transformJsonValueToObject(objectMapper, domainType.structure, jsonValue, domainFilters);
        } catch (JsonProcessingException e) {
            log.error(
                    String.format("Invalid Domain '%s' transform. Exceptions: %s", domainType.toJson(), e.getMessage())
            );
            return emptyList();
        }
    }

    public interface InputOptimizationRepository {

        Map<DomainType, String> getInputs(String warehouseId,
                                          Set<DomainType> domains);

    }

}
