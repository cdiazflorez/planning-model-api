package com.mercadolibre.planning.model.api.domain.entity;

import java.util.Locale;

public enum Path {
  SPD,
  FTL,
  FTLP,
  PRIVATE,
  SUPPLIER,
  COLLECT,
  PICKUP,
  TRANSFER,
  GLOBAL;

  public String getName() {
    return name().toLowerCase(Locale.ROOT);
  }

  public static Path from(final String value) {
    return valueOf(value.toUpperCase(Locale.getDefault()));
  }
}
