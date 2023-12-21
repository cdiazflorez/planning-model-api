package com.mercadolibre.planning.model.api.exception;

public class DateRangeBoundsException extends RuntimeException {

  private static final String MESSAGE_PATTERN = "Date range boundaries exceeded: %s";

  private static final long serialVersionUID = 1L;

  public DateRangeBoundsException(final String message) {
    super(String.format(MESSAGE_PATTERN, message));
  }

}
