package com.mercadolibre.planning.model.api.web.controller.projection.response;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import java.time.Instant;
import lombok.Value;

/**
 * Backlog projection by area result.
 */
@Value
public class BacklogProjectionByAreaDto {

  Instant operatingHour;

  ProcessName process;

  String area;

  BacklogProjectionStatus status;

  long quantity;

}
