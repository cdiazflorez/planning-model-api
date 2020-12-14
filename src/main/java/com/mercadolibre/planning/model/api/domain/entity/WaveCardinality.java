package com.mercadolibre.planning.model.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public enum WaveCardinality {

    MONO_ORDER_DISTRIBUTION,
    MULTI_BATCH_DISTRIBUTION,
    MULTI_ORDER_DISTRIBUTION;

    private static final Map<String, WaveCardinality> LOOKUP = Arrays.stream(values()).collect(
            toMap(WaveCardinality::toString, Function.identity())
    );

    public static Optional<WaveCardinality> of(final String value) {
        return Optional.ofNullable(LOOKUP.get(value.toUpperCase()));
    }

    @JsonValue
    public String toJson() {
        return toString().toLowerCase();
    }
}
