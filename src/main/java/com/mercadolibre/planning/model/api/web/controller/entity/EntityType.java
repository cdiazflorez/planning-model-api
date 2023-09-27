package com.mercadolibre.planning.model.api.web.controller.entity;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.ORDERS;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum EntityType {
    HEADCOUNT(WORKERS),
    HEADCOUNT_SYSTEMIC(WORKERS),
    HEADCOUNT_NON_SYSTEMIC(WORKERS),
    PRODUCTIVITY(UNITS_PER_HOUR),
    THROUGHPUT(UNITS_PER_HOUR),
    REMAINING_PROCESSING(UNITS),
    PERFORMED_PROCESSING(UNITS),
    BACKLOG_LOWER_LIMIT(UNITS),
    BACKLOG_UPPER_LIMIT(UNITS),
    BACKLOG_LOWER_LIMIT_SHIPPING(ORDERS),
    BACKLOG_UPPER_LIMIT_SHIPPING(ORDERS),
    MAX_CAPACITY(UNITS_PER_HOUR);

    private final MetricUnit metricUnit;

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
