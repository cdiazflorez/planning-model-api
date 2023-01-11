package com.mercadolibre.planning.model.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum BacklogGrouper {
  AREA,
  CARRIER,
  DATE_IN,
  DATE_OUT,
  STEP,
  STATUS,
  WORKFLOW;

  @JsonCreator
  public String getName() {
    return this.toString().toLowerCase(Locale.ROOT);
  }
}
