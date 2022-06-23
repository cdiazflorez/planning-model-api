package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum DeferralStatus {
  NOT_DEFERRED,
  DEFERRED_CAP_MAX,
  DEFERRED_CASCADE;

  @JsonValue
  public String toJson() {
    return toString().toLowerCase(Locale.US);
  }
}
