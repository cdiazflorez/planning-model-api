package com.mercadolibre.planning.model.api.exception;

public class DeviationsToSaveNotFoundException extends RuntimeException {

  public static final String MESSAGE_PATTERN = "Not found deviations to save.";
  private static final long serialVersionUID = 5800500956721253234L;

  @Override
  public String getMessage() {
    return MESSAGE_PATTERN;
  }

}
