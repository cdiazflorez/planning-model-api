package com.mercadolibre.planning.model.api.domain.entity.plan;

import java.util.Map;

public record StaffingPlanResponse(
    double value,
    double originalValue,
    Map<String, String> groupers) {
}
