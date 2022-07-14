package com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.Domain;
import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.ShiftParameters;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DomainShiftParameter implements DomainStrategy{

    private static final Map<String, Function<List<Object>, Predicate<? extends Domain>>> SHIFT_PARAMETERS_PREDICATES =
            Map.of(
                    "include_day_name", DomainShiftParameter::shiftParameterIncludeDayName,
                    "include_shift_type", DomainShiftParameter::shiftParameterIncludeShiftType
            );
    @Override
    public List<ShiftParameters> transformJsonValueToObject(final ObjectMapper objectMapper,
                                              final String jsonValue,
                                              final Map<String, List<Object>> domainFilters)
            throws JsonProcessingException {
        final List<ShiftParameters> resultDomains = objectMapper.readValue(jsonValue, new TypeReference<>() {});
        if (domainFilters == null) {
            return resultDomains;
        } else {
            return resultDomains.stream()
                    .filter((Predicate<? super ShiftParameters>) getDomainFilter(domainFilters))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public Predicate<? extends Domain> getDomainFilter(final Map<String, List<Object>> domainFilters) {
        return domainFilters.entrySet().stream()
                .map(domainFilter -> SHIFT_PARAMETERS_PREDICATES.get(domainFilter.getKey()).apply(domainFilter.getValue()))
                .reduce(domain -> true, Predicate::and);
    }

    private static Predicate<ShiftParameters> shiftParameterIncludeDayName(
            final List<Object> objectList) {
        final List<String> dayNames = objectList.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        return shiftParameters -> dayNames.contains(shiftParameters.getDayName());
    }

    private static Predicate<ShiftParameters> shiftParameterIncludeShiftType(final List<Object> objectList) {
        final List<String> shiftTypes = objectList.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        return shiftParameters -> shiftTypes.contains(shiftParameters.getShiftType());
    }
}
