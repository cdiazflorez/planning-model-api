package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import java.time.Instant;
import lombok.Value;

/**
 * Backlog representation by process and sla
 */
@Value
public class CurrentBacklogBySla {
  private ProcessName processName;

  private Instant dateOut;

  private int quantity;

}
