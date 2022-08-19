package com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.inputdomain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.InputCatalog;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class InputMultiple implements InputStrategy {

    @Override
    public Object transformJsonValueToObject(final ObjectMapper objectMapper,
                                             final Class<? extends InputCatalog> structure,
                                             final String jsonValue,
                                             final Map<String, List<Object>> domainFilters)
            throws JsonProcessingException {
        final List<InputCatalog> resultInputCatalogs = objectMapper.readValue(jsonValue,
                objectMapper.getTypeFactory().constructCollectionType(List.class, structure));
        if (domainFilters.isEmpty()) {
            return resultInputCatalogs;
        } else {
            return resultInputCatalogs.stream()
                    .filter(type -> type.conditionFilter(domainFilters))
                    .collect(Collectors.toList());
        }
    }
}
