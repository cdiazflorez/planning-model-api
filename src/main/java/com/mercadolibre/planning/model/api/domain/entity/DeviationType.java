package com.mercadolibre.planning.model.api.domain.entity;

import java.util.Locale;

public enum DeviationType {
  UNITS,
  MINUTES;

  public String getName() {
    return name().toLowerCase(Locale.ROOT);
  }
}
