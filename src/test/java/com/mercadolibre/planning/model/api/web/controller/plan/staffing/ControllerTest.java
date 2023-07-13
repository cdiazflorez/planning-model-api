package com.mercadolibre.planning.model.api.web.controller.plan.staffing;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_ORDER;
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

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
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

  private static final String BASE_URL = "/logistic_center/{logistic_center_id}/plan/staffing/{entity}";

  private static final ZonedDateTime DATE_FROM = A_DATE_UTC;

  private static final ZonedDateTime DATE_TO = A_DATE_UTC.plusHours(5);

  private static final String LOGISTIC_CENTER_ID = "ARTW01";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private GetThroughputUseCase getThroughputUseCase;

  private static GetEntityInput throughputInput() {
    return GetEntityInput.builder()
        .warehouseId(LOGISTIC_CENTER_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .dateFrom(DATE_FROM.withFixedOffsetZone())
        .dateTo(DATE_TO.withFixedOffsetZone())
        .source(SIMULATION)
        .processName(List.of(PICKING))
        .processPaths(List.of(TOT_MONO, TOT_MULTI_BATCH, TOT_MULTI_ORDER))
        .simulations(emptyList())
        .viewDate(A_DATE_UTC.toInstant())
        .build();
  }

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

  private EntityOutput entityOutput(final ZonedDateTime date, final long quantity, final ProcessPath processPath) {
    return EntityOutput.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .processName(PICKING)
        .processPath(processPath)
        .date(date)
        .metricUnit(UNITS_PER_HOUR)
        .source(SIMULATION)
        .value(quantity)
        .build();
  }

  private List<EntityOutput> mockThroughputs() {
    return List.of(
        entityOutput(DATE_FROM, 25, TOT_MONO),
        entityOutput(DATE_FROM.plusHours(1), 50, TOT_MONO),
        entityOutput(DATE_FROM.plusHours(2), 75, TOT_MONO),
        entityOutput(DATE_FROM.plusHours(3), 100, TOT_MONO),
        entityOutput(DATE_FROM.plusHours(4), 125, TOT_MONO),
        entityOutput(DATE_FROM.plusHours(5), 150, TOT_MONO),
        entityOutput(DATE_FROM, 13, TOT_MULTI_ORDER),
        entityOutput(DATE_FROM.plusHours(1), 26, TOT_MULTI_ORDER),
        entityOutput(DATE_FROM.plusHours(2), 39, TOT_MULTI_ORDER),
        entityOutput(DATE_FROM.plusHours(3), 52, TOT_MULTI_ORDER),
        entityOutput(DATE_FROM.plusHours(4), 65, TOT_MULTI_ORDER),
        entityOutput(DATE_FROM.plusHours(5), 78, TOT_MULTI_ORDER),
        entityOutput(DATE_FROM, 10, TOT_MULTI_BATCH),
        entityOutput(DATE_FROM.plusHours(1), 20, TOT_MULTI_BATCH),
        entityOutput(DATE_FROM.plusHours(2), 30, TOT_MULTI_BATCH),
        entityOutput(DATE_FROM.plusHours(3), 40, TOT_MULTI_BATCH),
        entityOutput(DATE_FROM.plusHours(4), 50, TOT_MULTI_BATCH),
        entityOutput(DATE_FROM.plusHours(5), 60, TOT_MULTI_BATCH)
    );
  }

}
