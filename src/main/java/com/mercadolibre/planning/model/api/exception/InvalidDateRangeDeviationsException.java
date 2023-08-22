package com.mercadolibre.planning.model.api.exception;

import java.time.Instant;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InvalidDateRangeDeviationsException extends RuntimeException {

  private static final long serialVersionUID = 2L;

  private final Instant dateFrom;

  private final Instant dateTo;

  @Override
  public String getMessage() {
    return String.format(
        "DateFrom [%s] is after dateTo [%s]",
        this.dateFrom,
        this.dateTo
    );
  }
}
