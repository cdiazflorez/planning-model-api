package com.mercadolibre.planning.model.api.usecase.plan;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.plan.HistoricalDialogSpaRepository;
import com.mercadolibre.planning.model.api.domain.entity.plan.HistoricalDialogSpa;
import com.mercadolibre.planning.model.api.domain.usecase.plan.DialogLogRecord;
import com.mercadolibre.planning.model.api.domain.usecase.plan.DialogLogService;
import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DialogLogServiceTest {

  @Mock
  private HistoricalDialogSpaRepository historicalDialogSpaRepository;

  @InjectMocks
  private DialogLogService dialogLogService;

  @Test
  public void saveTest() {
    //GIVEN
    when(historicalDialogSpaRepository.save(any())).thenReturn(mockHistoricalDialogSpa());

    //WHEN
    DialogLogRecord response = dialogLogService.saveDialogRecord(mockHistoricalDialogData());

    //THEN
    Assertions.assertNotNull(response);
    Assertions.assertEquals(1L, response.getId());

  }


  private DialogLogRecord mockHistoricalDialogData() {
    return new DialogLogRecord(
        1L,
        Instant.parse("2022-10-28T14:00:00Z"),
        "{\"configuration\": \"test\"}",
        "{\"workers_inbound\": [{"
            + "\"epoch\": 0,"
            + "\"stage\": 0,"
            + "\"permanents_hour\": \"3.66\","
            + "\"polyvalents_hour\": \"0.0\","
            + "\"process\": \"inbound\","
            + "\"stage_name\": \"receiving\","
            + "\"epoch_ts\": \"2022-10-24T15:00:00Z\","
            + "\"shift_name\": \"tt\","
            + "\"total_hour\": \"46.97\","
            + "\"total_hour_trim\": \"46.97\""
            + "}]}",
        "",
        Instant.parse("2022-10-28T14:00:45Z"),
        "ARBA01");
  }

  private HistoricalDialogSpa mockHistoricalDialogSpa() {
    final DialogLogRecord dialogData = mockHistoricalDialogData();

    return new HistoricalDialogSpa(dialogData.getId(),
        dialogData.getRequestDate(),
        dialogData.getSpaRequest(),
        dialogData.getSpaResponse(),
        dialogData.getFrontResult(),
        dialogData.getResponseDate(),
        dialogData.getLogisticCenter());
  }


}
