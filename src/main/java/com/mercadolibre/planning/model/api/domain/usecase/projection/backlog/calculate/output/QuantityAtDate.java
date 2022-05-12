package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output;

import java.time.Instant;
import lombok.Value;

/**
 * Backlog quantity for a specific date.
 */
@Value
public class QuantityAtDate {
  Instant date;

  Integer quantity;
}
