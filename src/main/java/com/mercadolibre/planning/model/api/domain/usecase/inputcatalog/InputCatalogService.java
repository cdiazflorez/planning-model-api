package com.mercadolibre.planning.model.api.domain.usecase.inputcatalog;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId;
import com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.get.GetInputCatalog;
import com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.inputdomain.InputStrategy;
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
public class InputCatalogService {

    private ObjectMapper objectMapper;

    private InputCatalogRepository inputCatalogRepository;

    public Map<InputId, Object> getInputsCatalog(final GetInputCatalog getInputCatalog) {

        return buildMapResponse(
                inputCatalogRepository.getInputs(getInputCatalog.getWarehouseId(), getInputCatalog.getDomains().keySet()),
                getInputCatalog.getDomains()
        );

    }

    private Map<InputId, Object> buildMapResponse(final Map<InputId, String> domains,
                                                  final Map<InputId, Map<String, List<Object>>> domainFilters) {

        return domains.entrySet().stream()
                .collect(toMap(
                        Entry::getKey,
                        domain -> getDomainValue(domain.getKey(), domain.getValue(), domainFilters.getOrDefault(domain.getKey(), Map.of()))
                ));
    }

    private Object getDomainValue(final InputId inputId,
                                  final String jsonValue,
                                  final Map<String, List<Object>> domainFilters) {
        try {
            InputStrategy inputStrategy = inputId.inputStrategy;
            return inputStrategy.transformJsonValueToObject(objectMapper, inputId.structure, jsonValue, domainFilters);
        } catch (JsonProcessingException e) {
            log.error(
                    String.format("Invalid Domain '%s' transform. Exceptions: %s", inputId.toJson(), e.getMessage())
            );
            return emptyList();
        }
    }

    public interface InputCatalogRepository {

        Map<InputId, String> getInputs(String warehouseId,
                                       Set<InputId> domains);

    }

}
