package com.mercadolibre.planning.model.api.web.controller.plan;

import com.mercadolibre.planning.model.api.domain.usecase.plan.DialogLogRecord;
import java.time.Instant;
import lombok.Value;

@Value
public class HistoricalDialogRequest {

  Instant requestDate;
  String spaRequest;
  String spaResponse;
  String frontResult;
  Instant responseDate;
  String logisticCenter;

  public DialogLogRecord toHistoricalDialogInput() {
    return new DialogLogRecord(
        0L,
        this.getRequestDate(),
        this.getSpaRequest(),
        this.getSpaResponse(),
        this.getFrontResult(),
        this.getResponseDate(),
        this.getLogisticCenter()
    );
  }
}
