package com.mercadolibre.planning.model.api.projection.availablecapacity;

import com.mercadolibre.planning.model.api.projection.builder.SlaProjectionResult;

public record AvailableCapacity(int capacity, SlaProjectionResult projection) {
}
