package com.mercadolibre.planning.model.api.domain.usecase.entities.input;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EntitySearchFilters {
    PROCESSING_TYPE,
    ABILITY_LEVEL;

    @JsonValue
    public String toJson() {
        return toString().toLowerCase();
    }
}
