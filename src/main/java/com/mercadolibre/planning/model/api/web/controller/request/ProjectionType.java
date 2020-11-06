package com.mercadolibre.planning.model.api.web.controller.request;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public enum ProjectionType {

    BACKLOG,
    // Check if name should be DATE_OUT instead of CPT
    CPT;

    private static final Map<String, ProjectionType> LOOKUP = Arrays.stream(values()).collect(
            toMap(ProjectionType::toString, Function.identity())
    );

    public static Optional<ProjectionType> of(final String value) {
        return Optional.ofNullable(LOOKUP.get(value.toUpperCase()));
    }

    @JsonValue
    public String toJson() {
        return toString().toLowerCase();
    }
}