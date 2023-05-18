package com.mercadolibre.planning.model.api.projection.dto.response;

import java.time.Instant;
import java.util.List;
import lombok.Value;

@Value
public class BacklogProjectionTotalResponse {
  Instant date;
  List<Sla> sla;
}
