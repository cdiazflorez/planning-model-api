package com.mercadolibre.planning.model.api.exception;

import java.io.Serial;

public class ReadFuryConfigException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  public ReadFuryConfigException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public String getMessage() {
    return String.format(
        "The input obtained from the FuryConfig is invalid: %s",
        super.getMessage()
    );
  }

}
