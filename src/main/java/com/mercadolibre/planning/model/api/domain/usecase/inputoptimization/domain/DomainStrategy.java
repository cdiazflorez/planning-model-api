package com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.Domain;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public interface DomainStrategy {

    Object transformJsonValueToObject(final ObjectMapper objectMapper,
                                      final  String jsonValue,
                                      final Map<String, List<Object>> domainFilters)
            throws JsonProcessingException;

    Predicate<? extends Domain> getDomainFilter(final Map<String, List<Object>> domainFilters);

}
