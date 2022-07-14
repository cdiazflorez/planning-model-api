package com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.Domain;
import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.ShiftParameters;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class DomainConfiguration implements DomainStrategy{

    @Override
    public ShiftParameters transformJsonValueToObject(final ObjectMapper objectMapper,
                                        final String jsonValue,
                                        final Map<String, List<Object>> domainFilters)
            throws JsonProcessingException {
            return objectMapper.readValue(jsonValue, new TypeReference<>() {});
    }

    @Override
    public Predicate<? extends Domain> getDomainFilter(
            Map<String, List<Object>> domainFilters) {
        return null;
    }
}
