package com.mercadolibre.planning.model.api.web.controller.request;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public enum EntityType {
    HEADCOUNT,
    PRODUCTIVITY,
    THROUGHPUT;

    private static final Map<String, EntityType> LOOKUP = Arrays.stream(values()).collect(
            toMap(EntityType::toString, Function.identity())
    );

    public static Optional<EntityType> of(final String value) {
        return Optional.ofNullable(LOOKUP.get(value.toUpperCase()));
    }

    @JsonValue
    public String toJson() {
        return toString().toLowerCase();
    }
}
