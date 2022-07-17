package com.mercadolibre.planning.model.api.domain.usecase.inputoptimization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.Domain;
import java.util.List;
import java.util.Map;

public class DomainSingle implements DomainStrategy{

    @Override
    public Object transformJsonValueToObject(ObjectMapper objectMapper,
            Class<? extends Domain> structure, String jsonValue,
            Map<String, List<Object>> domainFilters) throws JsonProcessingException {
        return objectMapper.readValue(jsonValue, structure);
    }
}
