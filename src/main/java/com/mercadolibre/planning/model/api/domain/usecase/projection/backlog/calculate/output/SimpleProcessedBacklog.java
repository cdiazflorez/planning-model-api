package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output;

import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionService.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionService.ProcessedBacklog;
import lombok.Value;

/**
 * Backlog projection result value where the processed and carried over backlog is stored.
 *
 * @param <T> backlog type.
 */
@Value
public class SimpleProcessedBacklog<T extends Backlog> implements ProcessedBacklog<T> {
  T processed;

  T carryOver;
}
