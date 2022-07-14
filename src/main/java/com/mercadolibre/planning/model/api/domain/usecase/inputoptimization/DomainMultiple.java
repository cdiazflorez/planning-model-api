package com.mercadolibre.planning.model.api.domain.usecase.inputoptimization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType;
import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.Domain;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DomainMultiple<T> implements DomainStrategy{

    @Override
    public List<T> transformJsonValueToObject(final ObjectMapper objectMapper,
                                              final String jsonValue,
                                              final Map<String, List<Object>> domainFilters)
            throws JsonProcessingException {
        final List<T> resultDomains = objectMapper.readValue(jsonValue, new TypeReference<>() {});
        if (domainFilters == null) {
            return resultDomains;
        } else {
            return resultDomains;
        }
    }
}
