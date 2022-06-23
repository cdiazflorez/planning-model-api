package com.mercadolibre.planning.model.api.exception;

public class NotImplementedException extends RuntimeException {
  private static final String MESSAGE_TEMPLATE = "Not Implemented. %s";

  private static final long serialVersionUID = 5800400356721253964L;

  public NotImplementedException(final String message) {
    super(String.format(MESSAGE_TEMPLATE, message));
  }
}
