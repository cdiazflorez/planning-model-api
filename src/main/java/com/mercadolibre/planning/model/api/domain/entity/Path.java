package com.mercadolibre.planning.model.api.domain.entity;

import java.util.Locale;

public enum Path {
  SPD,
  FTL,
  PRIVATE,
  COLLECT,
  TRANSFER_SHIPMENT;

  public String getName() {
    return name().toLowerCase(Locale.ROOT);
  }
}
