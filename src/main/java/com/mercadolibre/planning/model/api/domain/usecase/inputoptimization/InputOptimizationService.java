package com.mercadolibre.planning.model.api.domain.usecase.inputoptimization;

import static java.util.Collections.emptyList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.planning.model.api.client.db.repository.inputoptimization.InputOptimizationRepository;
import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType;
import java.util.LinkedHashMap;
import java.util.Map;
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

        final Map<DomainType, Object> response = new LinkedHashMap<>();

        if (request.getDomains() == null || request.getDomains().isEmpty()) {
            inputOptimizationRepository.findAllByWarehouseId(request.getWarehouseId()).forEach(
                    input -> response.put(input.getDomain(), transformJsonValueToObject(input.getDomain(),
                                                                                        input.getJsonValue()))
            );
        } else {
            inputOptimizationRepository.findAllByWarehouseIdAndDomainIn(request.getWarehouseId(),
                    request.getDomains()).forEach(
                    input -> response.put(input.getDomain(), transformJsonValueToObject(input.getDomain(),
                                                                                        input.getJsonValue()))
            );
        }

        return response;
    }

    private Object transformJsonValueToObject(final DomainType domainType,
                                              final String jsonValue) {
        try {
            return objectMapper.readValue(jsonValue, domainType.domainStructure);
        } catch (JsonProcessingException e) {
            log.error(
                    String.format("Invalid Domain %s transform. Exceptions: %s", domainType.toJson(), e.getMessage())
            );
            return emptyList();
        }
    }

}
