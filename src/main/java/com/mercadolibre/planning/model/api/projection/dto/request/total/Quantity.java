package com.mercadolibre.planning.model.api.projection.dto.request.total;

import java.time.Instant;
import lombok.Value;

@Value
public class Quantity {
  Instant dateIn;
  Instant dateOut;
  int quantity;
}
