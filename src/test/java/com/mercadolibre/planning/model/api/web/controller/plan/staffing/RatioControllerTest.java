package com.mercadolibre.planning.model.api.web.controller.plan.staffing;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_ORDER;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.ThroughputService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = RatioController.class)
class RatioControllerTest {

  private static final String BASE_URL = "/logistic_center/{logistic_center_id}/plan/staffing/ratio/process_path/throughput";

  private static final Instant DATE_FROM = A_DATE_UTC.toInstant();

  private static final Instant DATE_MIDDLE = A_DATE_UTC.plusHours(1).toInstant();

  private static final Instant DATE_TO = A_DATE_UTC.plusHours(2).toInstant();

  private static final Map<ProcessPath, Map<ProcessName, Map<Instant, Double>>> THROUGHPUT_RATIOS = Map.of(
      TOT_MONO, Map.of(
          PICKING, Map.of(
              DATE_FROM, 0.33,
              DATE_MIDDLE, 0.25,
              DATE_TO, 0D
          )
      ),
      TOT_MULTI_BATCH, Map.of(
          PICKING, Map.of(
              DATE_FROM, 0.33,
              DATE_MIDDLE, 0.25,
              DATE_TO, 0D
          )
      ),
      TOT_MULTI_ORDER, Map.of(
          PICKING, Map.of(
              DATE_FROM, 0.33,
              DATE_MIDDLE, 0.50,
              DATE_TO, 0D
          )
      )
  );

  @Autowired
  private MockMvc mvc;

  @MockBean
  private ThroughputService service;

  @Test
  @DisplayName("get throughput ratio by process path works ok")
  void testGetThroughputRatioOk() throws Exception {
    // GIVEN
    when(service.getThroughputRatioByProcessPath(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        List.of(TOT_MONO, TOT_MULTI_BATCH, TOT_MULTI_ORDER),
        Set.of(PICKING),
        DATE_FROM,
        DATE_TO,
        DATE_FROM
    )).thenReturn(THROUGHPUT_RATIOS);

    // WHEN
    final ResultActions result = mvc.perform(
        get(BASE_URL, WAREHOUSE_ID)
            .contentType(APPLICATION_JSON)
            .param("workflow", "fbm-wms-outbound")
            .param("process_paths", "tot_mono", "tot_multi_batch", "tot_multi_order")
            .param("processes", "picking")
            .param("date_from", DATE_FROM.toString())
            .param("date_to", DATE_TO.toString())
            .param("view_date", DATE_FROM.toString())
    );

    // THEN
    result.andExpect(status().isOk())
        .andExpect(content().json(getResourceAsString("controller/plan/staffing/get_process_path_throughput_ratio_response.json")));
  }

}
