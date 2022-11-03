package com.mercadolibre.planning.model.api.web.controller.plan;

import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.usecase.plan.DialogLogRecord;
import com.mercadolibre.planning.model.api.domain.usecase.plan.DialogLogService;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@Slf4j
@WebMvcTest(HistoricalDialogController.class)
public class HistoricalDialogControllerTest {

  private static final String URL = "/planning/model/plan/historical_dialog_spa";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private DialogLogService dialogLogService;

  @Test
  public void saveTest() throws Exception {
    //GIVEN
    when(dialogLogService.saveDialogRecord(any())).thenReturn(mockHistoricalDialogData());

    //WHEN
    final ResultActions result = mvc.perform(
        post(URL)
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("post_historical_dialog.json"))
    );

    // THEN
    result.andExpect(status().isOk());
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
}
