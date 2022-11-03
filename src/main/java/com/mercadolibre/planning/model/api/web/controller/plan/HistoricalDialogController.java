package com.mercadolibre.planning.model.api.web.controller.plan;

import com.mercadolibre.planning.model.api.domain.usecase.plan.DialogLogRecord;
import com.mercadolibre.planning.model.api.domain.usecase.plan.DialogLogService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/plan/historical_dialog_spa")
public class HistoricalDialogController {

  private final DialogLogService dialogLogService;

  @PostMapping
  public ResponseEntity<DialogLogRecord> save(@RequestBody HistoricalDialogRequest request) {

    return ResponseEntity.ok(dialogLogService.saveDialogRecord(request.toHistoricalDialogInput()));
  }


}
