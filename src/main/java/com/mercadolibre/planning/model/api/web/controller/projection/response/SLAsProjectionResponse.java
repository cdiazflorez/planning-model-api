package com.mercadolibre.planning.model.api.web.controller.projection.response;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.projection.builder.SlaProjectionResult;

public record SLAsProjectionResponse(
    Workflow workflow,
    SlaProjectionResult projection) {
}
