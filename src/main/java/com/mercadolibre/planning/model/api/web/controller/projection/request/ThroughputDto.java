package com.mercadolibre.planning.model.api.web.controller.projection.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.Instant;
import lombok.Value;

/**
 * Throughput by date, workflow and process.
 */
@Value
public class ThroughputDto {
  private Workflow workflow;

  private Instant date;

  private ProcessName processName;

  private long value;
}
