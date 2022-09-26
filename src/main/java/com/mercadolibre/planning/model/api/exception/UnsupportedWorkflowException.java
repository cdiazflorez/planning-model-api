package com.mercadolibre.planning.model.api.exception;

public class UnsupportedWorkflowException extends RuntimeException {

  private static final long serialVersionUID = 4955135398329582928L;

  public UnsupportedWorkflowException() {
    super("Workflow not supported");
  }
}
