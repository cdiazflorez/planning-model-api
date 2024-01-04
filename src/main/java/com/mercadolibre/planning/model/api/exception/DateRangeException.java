package com.mercadolibre.planning.model.api.exception;

import static java.lang.String.format;

import java.time.Instant;

public class DateRangeException extends RuntimeException {

  public static final String MESSAGE_PATTERN = "date_to must be greater than date_from. "
      + "date_from: %s, date_to: %s";

  private static final long serialVersionUID = 5800500956721253975L;

  private final Instant dateFrom;

  private final Instant dateTo;

  public DateRangeException(final Instant dateFrom, final Instant dateTo) {
    this.dateFrom = dateFrom;
    this.dateTo = dateTo;
  }

  @Override
  public String getMessage() {
    return format(MESSAGE_PATTERN, dateFrom, dateTo);
  }
}
