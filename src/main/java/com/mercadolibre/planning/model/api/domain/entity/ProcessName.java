package com.mercadolibre.planning.model.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

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
    PACKING(PICKING, false),
    PACKING_REGULAR(PICKING, false),
    BATCH_SORTER(PICKING,false),
    PACKING_WALL(BATCH_SORTER,false),
    EXPEDITION(PACKING, false),
    WALL_IN(null, false),
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
