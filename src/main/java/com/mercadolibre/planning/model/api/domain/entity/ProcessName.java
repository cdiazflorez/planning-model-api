package com.mercadolibre.planning.model.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public enum ProcessName {

    // FBM WMS OUTBOUND
    WAVING,
    PICKING,
    PUT_TO_WALL,
    PACKING,
    EXPEDITION;

    private static final Map<String, ProcessName> LOOKUP = Arrays.stream(values()).collect(
            toMap(ProcessName::toString, Function.identity())
    );

    public static Optional<ProcessName> of(final String value) {
        return Optional.ofNullable(LOOKUP.get(value.toUpperCase()));
    }

    @JsonValue
    public String toJson() {
        return toString().toLowerCase();
    }
}
