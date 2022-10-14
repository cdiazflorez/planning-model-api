package com.mercadolibre.planning.model.api.web.controller.plan.staffing;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.exception.ForecastNotFoundException;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = Controller.class)
class ControllerTest {

  private static final String BASE_URL = "/flow/logistic_center/{logistic_center_id}/plan/staffing/{entity}";

  private static final ZonedDateTime DATE_FROM = A_DATE_UTC;

  private static final ZonedDateTime DATE_TO = A_DATE_UTC.plusHours(5);

  private static final String LOGISTIC_CENTER_ID = "ARTW01";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private GetThroughputUseCase getThroughputUseCase;

  @Test
  @DisplayName("get throughput by process path works ok")
  void testGetThroughputEntityOk() throws Exception {
    // GIVEN
    when(getThroughputUseCase.execute(throughputInput()))
        .thenReturn(mockThroughputs());

    // WHEN
    final ResultActions result = mvc.perform(
        get(BASE_URL, LOGISTIC_CENTER_ID, "throughput")
            .contentType(APPLICATION_JSON)
            .param("workflow", "fbm-wms-outbound")
            .param("process_paths", "tot_mono", "tot_multi_batch", "tot_multi_order")
            .param("processes", "picking")
            .param("date_from", DATE_FROM.toInstant().toString())
            .param("date_to", DATE_TO.toInstant().toString())
            .param("view_date", DATE_FROM.toInstant().toString())
    );

    // THEN
    result.andExpect(status().isOk())
        .andExpect(content().json(getResourceAsString("controller/plan/staffing/get_process_path_throughput_response.json")));
  }

  @Test
  @DisplayName("when get throughput by process path and no forecast is found then return 404 not found")
  void testGetThroughputEntityError() throws Exception {
    // GIVEN
    when(getThroughputUseCase.execute(throughputInput()))
        .thenThrow(ForecastNotFoundException.class);

    // WHEN
    final ResultActions result = mvc.perform(
        get(BASE_URL, LOGISTIC_CENTER_ID, "throughput")
            .contentType(APPLICATION_JSON)
            .param("workflow", "fbm-wms-outbound")
            .param("process_paths", "tot_mono", "tot_multi_batch", "tot_multi_order")
            .param("processes", "picking")
            .param("date_from", DATE_FROM.toInstant().toString())
            .param("date_to", DATE_TO.toInstant().toString())
            .param("view_date", DATE_FROM.toInstant().toString())
    );

    // THEN
    result.andExpect(status().isNotFound());
  }

  private static GetEntityInput throughputInput() {
    return GetEntityInput.builder()
        .warehouseId(LOGISTIC_CENTER_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .dateFrom(DATE_FROM.withFixedOffsetZone())
        .dateTo(DATE_TO.withFixedOffsetZone())
        .source(SIMULATION)
        .processName(List.of(PICKING))
        .simulations(emptyList())
        .viewDate(A_DATE_UTC.toInstant())
        .build();
  }

  private EntityOutput entityOutput(final ZonedDateTime date, final long quantity) {
    return EntityOutput.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .processName(PICKING)
        .date(date)
        .metricUnit(UNITS_PER_HOUR)
        .source(SIMULATION)
        .value(quantity)
        .build();
  }

  private List<EntityOutput> mockThroughputs() {
    return List.of(
        entityOutput(DATE_FROM, 100),
        entityOutput(DATE_FROM.plusHours(1), 200),
        entityOutput(DATE_FROM.plusHours(2), 300),
        entityOutput(DATE_FROM.plusHours(3), 400),
        entityOutput(DATE_FROM.plusHours(4), 500),
        entityOutput(DATE_FROM.plusHours(5), 600)
    );
  }

}
