package com.mercadolibre.planning.model.api.projection.dto.request.total;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Throughput {
  private final Instant date;
  private final Integer quantity;
}
