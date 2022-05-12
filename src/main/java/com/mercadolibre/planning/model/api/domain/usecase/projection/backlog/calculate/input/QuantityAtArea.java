package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input;

import lombok.Value;

/**
 * Backlog units in one specific area.
 */
@Value
public class QuantityAtArea {
  String area;

  double quantity;
}
