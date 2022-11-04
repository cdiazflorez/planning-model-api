package com.mercadolibre.planning.model.api.domain.usecase.plan;

import java.time.Instant;
import lombok.Value;

@Value
public class DialogLogRecord {

  Long id;
  Instant requestDate;
  String spaRequest;
  String spaResponse;
  String frontResult;
  Instant responseDate;
  String logisticCenter;
}
