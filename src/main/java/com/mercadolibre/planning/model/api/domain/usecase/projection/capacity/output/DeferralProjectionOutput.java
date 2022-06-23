package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.output;

import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.DeferralStatus;
import java.time.Instant;
import lombok.Value;

@Value
public class DeferralProjectionOutput {
  Instant sla;

  Instant deferredAt;

  int deferredUnits;

  DeferralStatus deferralStatus;
}
