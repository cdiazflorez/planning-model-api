package com.mercadolibre.planning.model.api.projection.dto.request.total;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Quantity{
  @JsonProperty("date_out")
  Instant dateOut;
  int quantity;
}
