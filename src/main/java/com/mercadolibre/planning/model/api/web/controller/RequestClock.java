package com.mercadolibre.planning.model.api.web.controller;

import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * A frozen clock that is created for each request.
 */
@Component
@RequestScope
public class RequestClock {
  private final Instant requestInstant;

  public RequestClock() {
    requestInstant = Instant.now();
  }

  /**
   * Gives the instant when the request associated to this thread was received.
   * @return the instant when the request was received.
   */
  public Instant now() {
    return requestInstant;
  }
}
