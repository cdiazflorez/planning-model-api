package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output;

import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionService.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionService.ProcessedBacklog;
import java.time.Instant;
import lombok.Value;

/**
 * Backlog projection result.
 *
 * @param <T> backlog type.
 */
@Value
public class ProjectionResult<T extends Backlog> {
  Instant operatingHour;

  ProcessedBacklog<T> resultingState;
}
