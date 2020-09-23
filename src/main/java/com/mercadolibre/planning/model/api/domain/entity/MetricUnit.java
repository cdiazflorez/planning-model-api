package com.mercadolibre.planning.model.api.domain.entity;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@SuppressWarnings("PMD.ShortMethodName")
public enum MetricUnit {

    MINUTES,
    PERCENTAGE,
    UNITS,
    UNITS_PER_HOUR,
    WORKERS;

    private static final Map<String, MetricUnit> LOOKUP = Arrays.stream(values()).collect(
            toMap(MetricUnit::toString, Function.identity())
    );

    public static Optional<MetricUnit> of(final String value) {
        return Optional.ofNullable(LOOKUP.get(value.toUpperCase()));
    }
}
