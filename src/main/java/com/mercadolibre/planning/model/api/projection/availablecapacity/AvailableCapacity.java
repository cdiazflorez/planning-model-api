package com.mercadolibre.planning.model.api.projection.availablecapacity;

import java.util.List;

public record AvailableCapacity(List<CapacityBySLA> capacities) {
}
