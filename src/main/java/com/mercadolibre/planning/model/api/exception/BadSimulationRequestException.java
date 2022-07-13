package com.mercadolibre.planning.model.api.exception;

public class BadSimulationRequestException extends RuntimeException {

  public static final long serialVersionUID = -7034897190745766999L;

  public static final String MESSAGE_PATTERN = "Duplicate SimulationEntity with name %s";

  private final String entityName;

  public BadSimulationRequestException(final String entityName) {
    this.entityName = entityName;
  }

  @Override
  public String getMessage() {
    return String.format(MESSAGE_PATTERN, entityName);
  }
}
