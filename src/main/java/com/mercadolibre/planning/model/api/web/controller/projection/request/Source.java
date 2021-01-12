package com.mercadolibre.planning.model.api.web.controller.projection.request;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public enum Source {
    FORECAST,
    SIMULATION;

    private static final Map<String, Source> LOOKUP = Arrays.stream(values()).collect(
            toMap(Source::toString, Function.identity())
    );

    public static Optional<Source> of(final String value) {
        return Optional.ofNullable(LOOKUP.get(value.toUpperCase()));
    }

    @JsonValue
    public String toJson() {
        return toString().toLowerCase();
    }
}
