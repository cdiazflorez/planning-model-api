package com.mercadolibre.planning.model.api.domain.service.lastupdatedentity;

import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import java.time.Instant;
import java.util.Map;

public record LastModifiedDates(Instant lastStaffingCreated,
                                Map<EntityType, Instant> lastDateEntitiesUpdate) {
}
