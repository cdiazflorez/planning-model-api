package com.mercadolibre.planning.model.api.domain.usecase.deferral;

import java.util.Locale;

public enum DeferralType {
  CAP_MAX,
  CASCADE;

  public String getName() {
    return name().toLowerCase(Locale.ROOT);
  }
}
