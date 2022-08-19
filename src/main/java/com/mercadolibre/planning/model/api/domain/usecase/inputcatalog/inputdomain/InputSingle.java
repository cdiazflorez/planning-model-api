package com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.inputdomain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.InputCatalog;
import java.util.List;
import java.util.Map;

public class InputSingle implements InputStrategy {

    @Override
    public Object transformJsonValueToObject(final ObjectMapper objectMapper,
                                             final Class<? extends InputCatalog> structure,
                                             final String jsonValue,
                                             final Map<String, List<Object>> domainFilters) throws JsonProcessingException {
        return objectMapper.readValue(jsonValue, structure);
    }
}
