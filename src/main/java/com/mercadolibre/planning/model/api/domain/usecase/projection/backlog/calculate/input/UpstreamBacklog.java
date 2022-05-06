package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input;

import static com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.BacklogBySla.emptyBacklog;

import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionService.IncomingBacklog;
import java.time.Instant;
import java.util.Map;
import lombok.Value;

/**
 * Upstream backlog as resulting from a backlog by cpt projection.
 */
@Value
public class UpstreamBacklog implements IncomingBacklog<BacklogBySla> {

  Map<Instant, BacklogBySla> plannedUnits;

  public BacklogBySla get(final Instant operatingHour) {
    return plannedUnits.getOrDefault(operatingHour, emptyBacklog());
  }
}
