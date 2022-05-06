package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.helper;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionService.Backlog;

/**
 * Applies transformation from one backlog representation to another.
 * @param <I> Initial backlog representation.
 * @param <O> Final backlog representation.
 */
public interface BacklogMapper<I extends Backlog, O extends Backlog> {
  O map(ProcessName process, I backlog);
}
