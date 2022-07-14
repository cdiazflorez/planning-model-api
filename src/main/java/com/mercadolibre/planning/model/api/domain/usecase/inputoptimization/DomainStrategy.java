package com.mercadolibre.planning.model.api.domain.usecase.inputoptimization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

public interface DomainStrategy {

    Object transformJsonValueToObject(final ObjectMapper objectMapper,
                                      final  String jsonValue,
                                      final Map<String, List<Object>> domainFilters)
            throws JsonProcessingException;

}
