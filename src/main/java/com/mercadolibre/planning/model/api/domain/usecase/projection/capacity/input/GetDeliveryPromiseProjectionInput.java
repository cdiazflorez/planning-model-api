package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
@Builder
public class GetDeliveryPromiseProjectionInput {

    String warehouseId;

    Workflow workflow;

    ProjectionType projectionType;

    ZonedDateTime dateFrom;

    ZonedDateTime dateTo;

    List<Backlog> backlog;

    String timeZone;

    boolean applyDeviation;
}
