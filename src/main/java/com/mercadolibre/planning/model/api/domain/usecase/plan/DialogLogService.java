package com.mercadolibre.planning.model.api.domain.usecase.plan;

import com.mercadolibre.planning.model.api.client.db.repository.plan.HistoricalDialogSpaRepository;
import com.mercadolibre.planning.model.api.domain.entity.plan.HistoricalDialogSpa;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DialogLogService {

  private final HistoricalDialogSpaRepository historicalDialogSpaRepository;

  public DialogLogRecord saveDialogRecord(DialogLogRecord dialogLogRecord) {

    HistoricalDialogSpa response = historicalDialogSpaRepository.save(new HistoricalDialogSpa(
        null,
        dialogLogRecord.getRequestDate(),
        dialogLogRecord.getSpaRequest(),
        dialogLogRecord.getSpaResponse(),
        dialogLogRecord.getFrontResult(),
        dialogLogRecord.getResponseDate(),
        dialogLogRecord.getLogisticCenter()));

    return new DialogLogRecord(
        response.getId(),
        response.getRequestDate(),
        response.getSpaRequest(),
        response.getSpaResponse(),
        response.getFrontResult(),
        response.getResponseDate(),
        response.getLogisticCenter()
    );
  }


}
