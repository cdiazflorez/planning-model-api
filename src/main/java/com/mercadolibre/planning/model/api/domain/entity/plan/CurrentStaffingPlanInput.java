package com.mercadolibre.planning.model.api.domain.entity.plan;

import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CurrentStaffingPlanInput(
    String logisticCenterId,
    Workflow workflow,
    Instant dateFrom,
    Instant dateTo,
    ProcessingType type,
    List<String> groupers,
    Map<String, List<String>> filters
) {
}
