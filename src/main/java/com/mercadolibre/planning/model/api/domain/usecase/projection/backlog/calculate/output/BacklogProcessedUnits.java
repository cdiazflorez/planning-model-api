package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output;

import java.time.ZonedDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class BacklogProcessedUnits {

  private BacklogProjection backlogProjection;
  private Map<ZonedDateTime, Long> processedUnits;

}
