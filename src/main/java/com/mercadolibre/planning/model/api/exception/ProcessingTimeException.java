package com.mercadolibre.planning.model.api.exception;

public class ProcessingTimeException extends RuntimeException {

  private static final String MESSAGE_PATTERN = "Something went wrong with the OutboundProcessingTime request: %s";

      private static final long serialVersionUID = 1L;

  public ProcessingTimeException(final String message, final Throwable cause) {
    super(String.format(MESSAGE_PATTERN, message), cause);
  }
}
