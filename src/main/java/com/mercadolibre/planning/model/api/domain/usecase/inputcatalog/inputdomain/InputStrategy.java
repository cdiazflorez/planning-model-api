package com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.inputdomain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.InputCatalog;
import java.util.List;
import java.util.Map;

public interface InputStrategy {

    Object transformJsonValueToObject(ObjectMapper objectMapper,
                                      Class<? extends InputCatalog> structure,
                                      String jsonValue,
                                      Map<String, List<Object>> domainFilters)
            throws JsonProcessingException;

}
