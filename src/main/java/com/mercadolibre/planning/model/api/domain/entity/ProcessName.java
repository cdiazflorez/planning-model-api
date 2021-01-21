package com.mercadolibre.planning.model.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;

@Getter
@AllArgsConstructor
public enum ProcessName {

    // FBM WMS OUTBOUND
    WAVING(null, false),
    PICKING(singletonList(WAVING), true),
    PACKING(singletonList(PICKING), false),
    PUT_TO_WALL(singletonList(PICKING), false),
    EXPEDITION(List.of(PACKING, PUT_TO_WALL), false);

    private final List<ProcessName> previousProcesses;
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
