package com.mercadolibre.planning.model.api.projection.dto.request.total;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class QuantityPlanned extends Quantity {
  @JsonProperty("date_out")
  Instant dateIn;

  public QuantityPlanned(Instant dateOut, int quantity, Instant dateIn) {
    super(dateOut, quantity);
    this.dateIn = dateIn;
  }
}
