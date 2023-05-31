package com.mercadolibre.planning.model.api.projection.dto.response.total;

import java.time.Instant;
import java.util.List;
import lombok.Value;

@Value
public class BacklogProjectionTotalResponse {
  Instant date;
  List<SlaTotal> sla;
}
