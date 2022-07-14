package com.mercadolibre.planning.model.api.domain.usecase.inputoptimization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DomainSingle<T> implements DomainStrategy{

    @Override
    public T transformJsonValueToObject(final ObjectMapper objectMapper,
                                        final String jsonValue,
                                        final Map<String, List<Object>> domainFilters)
            throws JsonProcessingException {
            return objectMapper.readValue(jsonValue, new TypeReference<>() {});
    }
}
