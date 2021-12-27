package com.mercadolibre.planning.model.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PUT_AWAY;
import static java.lang.Math.min;
import static java.util.stream.Collectors.toMap;

@Getter
@AllArgsConstructor
public enum Workflow {
    FBM_WMS_INBOUND(Workflow::calculateInboundCapacity),
    FBM_WMS_OUTBOUND(Workflow::calculateOutboundCapacity);

    private static final Map<String, Workflow> LOOKUP = Arrays.stream(values()).collect(
            toMap(Workflow::toString, Function.identity())
    );

    private final Function<Map<ProcessName, Long>, Long> capacityCalculator;

    @JsonCreator
    public static Optional<Workflow> of(final String value) {
        return Optional.ofNullable(LOOKUP.get(value.toUpperCase().replace('-', '_')));
    }

    private static long calculateInboundCapacity(final Map<ProcessName, Long> capacities) {
        return min(
                capacities.getOrDefault(CHECK_IN, 0L),
                capacities.getOrDefault(PUT_AWAY, 0L)
        );
    }

    private static long calculateOutboundCapacity(final Map<ProcessName, Long> capacities) {
        return min(
                capacities.getOrDefault(PICKING, 0L),
                capacities.getOrDefault(PACKING, 0L)
                        + capacities.getOrDefault(PACKING_WALL, 0L)
        );
    }

    @JsonValue
    public String toJson() {
        return toString().toLowerCase().replace('_', '-');
    }
}
