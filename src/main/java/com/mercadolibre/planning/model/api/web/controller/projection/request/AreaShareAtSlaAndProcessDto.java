package com.mercadolibre.planning.model.api.web.controller.projection.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import java.time.Instant;
import lombok.Value;

/**
 * Backlog by area distribution for one sla.
 */
@Value
public class AreaShareAtSlaAndProcessDto {
  private ProcessName processName;

  private Instant date;

  private String area;

  private Double percentage;
}
