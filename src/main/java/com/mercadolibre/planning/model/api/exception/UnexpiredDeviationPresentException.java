package com.mercadolibre.planning.model.api.exception;

public class UnexpiredDeviationPresentException extends RuntimeException {
  private static final long serialVersionUID = 5800500956721253234L;

  private final String message;

  public UnexpiredDeviationPresentException(final String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return this.message;
  }
}
