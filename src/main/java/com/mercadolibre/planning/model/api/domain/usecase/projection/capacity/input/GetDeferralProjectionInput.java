package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.Value;

@Value
public class GetDeferralProjectionInput {
  String logisticCenterId;

  Workflow workflow;

  ProjectionType projectionType;

  Instant viewDate;

  ZonedDateTime dateFrom;

  ZonedDateTime dateTo;

  ZonedDateTime slaFrom;

  ZonedDateTime slaTo;

  List<Backlog> backlog;

  String timeZone;

  boolean applyDeviation;
}
