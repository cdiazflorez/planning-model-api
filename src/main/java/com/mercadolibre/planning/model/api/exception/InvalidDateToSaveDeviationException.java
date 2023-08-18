package com.mercadolibre.planning.model.api.exception;

import static java.lang.String.format;

import java.time.Instant;

public class InvalidDateToSaveDeviationException extends RuntimeException {

  private static final String MESSAGE_PATTERN = "The date range dateFrom: %s and dateTo: %s must be greater than the currentDate: %s";
  private static final long serialVersionUID = 5800500956721253912L;
  private final Instant currentDate;
  private final Instant dateFrom;
  private final Instant dateTo;

  public InvalidDateToSaveDeviationException(final Instant dateFrom, final Instant dateTo, final Instant currentDate) {
    this.currentDate = currentDate;
    this.dateFrom = dateFrom;
    this.dateTo = dateTo;
  }


  @Override
  public String getMessage() {
    return format(MESSAGE_PATTERN, this.dateFrom, this.dateTo, this.currentDate);
  }
}
