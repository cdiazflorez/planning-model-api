package com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.inputdomain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.Domain;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DomainMultiple implements DomainStrategy {

    @Override
    public Object transformJsonValueToObject(final ObjectMapper objectMapper,
                                             final Class<? extends Domain> structure,
                                             final String jsonValue,
                                             final Map<String, List<Object>> domainFilters)
            throws JsonProcessingException {
        final List<Domain> resultDomains = objectMapper.readValue(jsonValue,
                objectMapper.getTypeFactory().constructCollectionType(List.class, structure));
        if (domainFilters == null || domainFilters.isEmpty()) {
            return resultDomains;
        } else {
            return resultDomains.stream()
                    .filter(type -> type.conditionFilter(domainFilters))
                    .collect(Collectors.toList());
        }
    }
}
