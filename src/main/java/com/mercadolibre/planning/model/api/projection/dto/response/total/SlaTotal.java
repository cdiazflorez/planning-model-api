package com.mercadolibre.planning.model.api.projection.dto.response.total;

import com.mercadolibre.planning.model.api.projection.dto.response.ProcessPathResponse;
import java.time.Instant;
import java.util.List;
import lombok.Value;

@Value
public class SlaTotal {
  Instant dateOut;
  int quantity;
  List<ProcessPathResponse> processPath;
}
