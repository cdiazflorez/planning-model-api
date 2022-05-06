package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input;

import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionService.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.QuantityAtDate;
import java.util.Collections;
import java.util.List;
import lombok.Value;

/**
 * Backlog representation by sla for a specific process.
 */
@Value
public class BacklogBySla implements Backlog {

  private static final BacklogBySla EMPTY_BACKLOG = new BacklogBySla(Collections.emptyList());

  List<QuantityAtDate> distributions;

  public static BacklogBySla emptyBacklog() {
    return EMPTY_BACKLOG;
  }
}
