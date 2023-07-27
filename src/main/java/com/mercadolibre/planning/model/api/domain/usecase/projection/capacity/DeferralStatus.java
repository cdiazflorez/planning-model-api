package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType;
import java.util.Locale;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum DeferralStatus {
  NOT_DEFERRED(DeferralType.NOT_DEFERRED),
  DEFERRED_CAP_MAX(DeferralType.CAP_MAX),
  DEFERRED_CASCADE(DeferralType.CASCADE);

  private DeferralType deferralType;

  public DeferralType getDeferralType() {
    return deferralType;
  }

  @JsonValue
  public String toJson() {
    return toString().toLowerCase(Locale.US);
  }
}
