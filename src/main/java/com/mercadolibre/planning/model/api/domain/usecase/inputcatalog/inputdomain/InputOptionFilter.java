package com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.inputdomain;

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
public enum InputOptionFilter {
    INCLUDE_DAY_NAME,
    INCLUDE_SHIFT_GROUP,
    INCLUDE_PROCESS,
    INCLUDE_STAGE;

    private static final Map<String, InputOptionFilter> LOOKUP = Arrays.stream(values()).collect(
            toMap(InputOptionFilter::toString, Function.identity())
    );

    public static Optional<InputOptionFilter> of(final String value) {
        return ofNullable(LOOKUP.get(value.toUpperCase(Locale.getDefault())));
    }

    @JsonValue
    public String toJson() {
        return toString().toLowerCase(Locale.getDefault());
    }

}
