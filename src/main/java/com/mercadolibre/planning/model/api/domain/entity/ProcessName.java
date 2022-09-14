package com.mercadolibre.planning.model.api.domain.entity;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProcessName {
    // FBM WMS INBOUND
    RECEIVING(null, false),
    STAGE_IN(RECEIVING, false),
    CHECK_IN(RECEIVING, false),
    PUT_AWAY(CHECK_IN, false),

    // FBM WMS OUTBOUND
    WAVING(null, false),
    PICKING(WAVING, true),
    PACKING(PICKING, true),
    BATCH_SORTER(PICKING, true),
    WALL_IN(BATCH_SORTER, true),
    PACKING_WALL(WALL_IN, true),
    EXPEDITION(PACKING, false),
    GLOBAL(null, false);

    private final ProcessName previousProcesses;

    private final boolean considerPreviousBacklog;

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
