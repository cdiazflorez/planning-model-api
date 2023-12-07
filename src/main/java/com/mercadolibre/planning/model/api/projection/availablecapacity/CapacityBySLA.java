package com.mercadolibre.planning.model.api.projection.availablecapacity;

import java.time.Instant;

public record CapacityBySLA(Instant date, Integer capacity) {
}
