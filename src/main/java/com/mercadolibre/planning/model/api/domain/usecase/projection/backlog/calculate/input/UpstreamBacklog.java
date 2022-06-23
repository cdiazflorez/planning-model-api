package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input;

import static com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.BacklogBySla.emptyBacklog;

import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionService.IncomingBacklog;
import com.mercadolibre.planning.model.api.exception.NotImplementedException;
import java.time.Instant;
import java.util.Map;
import lombok.Value;

/**
 * Upstream backlog as resulting from a backlog by cpt projection.
 */
@Value
public class UpstreamBacklog implements IncomingBacklog<BacklogBySla> {

  Map<Instant, BacklogBySla> plannedUnits;

  @Override
  public BacklogBySla get(final Instant operatingHour) {
    return plannedUnits.getOrDefault(operatingHour, emptyBacklog());
  }

  @Override
  public BacklogBySla get(Instant dateFrom, Instant dateTo) {
    throw new NotImplementedException("UpstreamBacklog get between dates");
  }
}
