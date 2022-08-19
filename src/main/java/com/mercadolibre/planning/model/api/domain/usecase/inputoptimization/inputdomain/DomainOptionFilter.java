package com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.inputdomain;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum DomainOptionFilter {
    INCLUDE_DAY_NAME,
    INCLUDE_SHIFT_TYPE,
    INCLUDE_PROCESS,
    INCLUDE_STAGE;

    private static final Map<String, DomainOptionFilter> LOOKUP = Arrays.stream(values()).collect(
            toMap(DomainOptionFilter::toString, Function.identity())
    );

    public static Optional<DomainOptionFilter> of(final String value) {
        return ofNullable(LOOKUP.get(value.toUpperCase(Locale.getDefault())));
    }

    @JsonValue
    public String toJson() {
        return toString().toLowerCase(Locale.getDefault());
    }

}
