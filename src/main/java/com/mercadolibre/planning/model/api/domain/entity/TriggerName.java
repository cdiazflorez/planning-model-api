package com.mercadolibre.planning.model.api.domain.entity;

import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public enum TriggerName {
    SLA,
    IDLENESS;

    private static final Map<String, TriggerName> LOOKUP = Arrays.stream(values()).collect(
            toMap(TriggerName::toString, Function.identity())
    );

    public static Optional<TriggerName> of(final String value) {
        return Optional.ofNullable(LOOKUP.get(value.toUpperCase(Locale.US)));
    }
}

