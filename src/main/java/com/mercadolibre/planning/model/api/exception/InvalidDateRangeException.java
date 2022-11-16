package com.mercadolibre.planning.model.api.exception;

import static java.lang.String.format;

import java.time.Instant;

public class InvalidDateRangeException extends RuntimeException {

  public static final String MESSAGE_PATTERN = "The range of dates of entry or exit is not present. "
      + "dateInFrom: %s, dateInTo: %s, dateOutFrom: %s, dateOutTo: %s";
  private static final long serialVersionUID = 5800500956721253975L;

  private final Instant dateInFrom;
  private final Instant dateInTo;
  private final Instant dateOutFrom;
  private final Instant dateOutTo;


  public InvalidDateRangeException(final Instant dateInFrom, final Instant dateInTo, final Instant dateOutFrom, final Instant dateOutTo) {
    this.dateInFrom = dateInFrom;
    this.dateInTo = dateInTo;
    this.dateOutFrom = dateOutFrom;
    this.dateOutTo = dateOutTo;
  }

  @Override
  public String getMessage() {
    return format(MESSAGE_PATTERN, dateInFrom, dateInTo, dateOutFrom, dateOutTo);
  }
}
