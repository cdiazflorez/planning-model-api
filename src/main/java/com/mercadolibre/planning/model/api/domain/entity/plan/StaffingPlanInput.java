package com.mercadolibre.planning.model.api.domain.entity.plan;

import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record StaffingPlanInput(
    List<Long> forecastIds,
    Instant dateFrom,
    Instant dateTo,
    ProcessingType type,
    List<String> groupers,
    Map<String, List<Object>> filters
) {
}
