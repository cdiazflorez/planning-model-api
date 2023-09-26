package com.mercadolibre.planning.model.api.web.controller.plan.staffing;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_ORDER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.ProductivityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.exception.ForecastNotFoundException;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
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

  private static final String BASE_URL = "/logistic_center/{logistic_center_id}/plan/staffing";

  private static final ZonedDateTime DATE_FROM = A_DATE_UTC;

  private static final ZonedDateTime DATE_TO = A_DATE_UTC.plusHours(5);

  private static final String LOGISTIC_CENTER_ID = "ARTW01";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private GetThroughputUseCase getThroughputUseCase;

  @MockBean
  private GetProductivityEntityUseCase getProductivityEntityUseCase;

  @MockBean
  private GetHeadcountEntityUseCase getHeadcountEntityUseCase;

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

    final String url = BASE_URL + "/throughput";

    // WHEN
    final ResultActions result = mvc.perform(
        get(url, LOGISTIC_CENTER_ID)
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

    final String url = BASE_URL + "/throughput";

    // WHEN
    final ResultActions result = mvc.perform(
        get(url, LOGISTIC_CENTER_ID)
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

  @Test
  void testUpdateStaffingPlan() throws Exception {
    // WHEN
    final ResultActions result = mvc.perform(
        put(BASE_URL, LOGISTIC_CENTER_ID)
            .param("user_id", "123")
            .param("workflow", "fbm-wms-outbound")
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("controller/plan/staffing/put_plan_staffing_entities_request.json"))

    );

    // THEN
    result.andExpect(status().isCreated());
  }


  @Test
  @DisplayName("get all entities by process path works ok")
  void testGetAllEntities() throws Exception {
    // GIVEN
    when(getHeadcountEntityUseCase.execute(any(GetHeadcountInput.class)))
        .thenReturn(mockHeadcount());

    when(getProductivityEntityUseCase.execute(any(GetProductivityInput.class)))
        .thenReturn(mockProductivity());

    when(getThroughputUseCase.execute(any(GetEntityInput.class)))
        .thenReturn(mockThroughputs());

    // WHEN
    final ResultActions result = mvc.perform(
        get(BASE_URL, LOGISTIC_CENTER_ID)
            .contentType(APPLICATION_JSON)
            .param("workflow", "fbm-wms-outbound")
            .param("processes", "picking")
            .param("date_from", DATE_FROM.toInstant().toString())
            .param("date_to", DATE_TO.toInstant().toString())
            .param("view_date", DATE_FROM.toInstant().toString())
    );

    // THEN
    result.andExpect(status().isOk())
        .andExpect(content().json(getResourceAsString("controller/plan/staffing/get_all_plan_staffing_entities_response.json")));
  }

  @Test
  @DisplayName("when get all entities by process path and no forecast is found then return 404 not found")
  void testGetAllEntitiesError() throws Exception {
    // GIVEN
    when(getHeadcountEntityUseCase.execute(any(GetHeadcountInput.class)))
        .thenThrow(ForecastNotFoundException.class);

    // WHEN
    final ResultActions result = mvc.perform(
        get(BASE_URL, LOGISTIC_CENTER_ID)
            .contentType(APPLICATION_JSON)
            .param("workflow", "fbm-wms-outbound")
            .param("processes", "picking")
            .param("date_from", DATE_FROM.toInstant().toString())
            .param("date_to", DATE_TO.toInstant().toString())
            .param("view_date", DATE_FROM.toInstant().toString())
    );

    // THEN
    result.andExpect(status().isNotFound());
  }

  private EntityOutput entityOutput(
      final ZonedDateTime date,
      final long quantity,
      final ProcessPath processPath,
      final Source source,
      final boolean isThroughput
  ) {
    return EntityOutput.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .processName(PICKING)
        .processPath(processPath)
        .date(date)
        .type(isThroughput ? THROUGHPUT : EFFECTIVE_WORKERS)
        .metricUnit(isThroughput ? UNITS_PER_HOUR : WORKERS)
        .source(source)
        .value(quantity)
        .build();
  }

  private ProductivityOutput productivityOutput(
      final ZonedDateTime date,
      final double quantity,
      final ProcessPath processPath,
      final Source source,
      final int abilityLevel
  ) {
    return ProductivityOutput.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .processName(PICKING)
        .processPath(processPath)
        .date(date)
        .metricUnit(UNITS_PER_HOUR)
        .source(source)
        .value(quantity)
        .abilityLevel(abilityLevel)
        .build();
  }

  private List<EntityOutput> mockThroughputs() {
    return List.of(
        entityOutput(DATE_FROM, 25, TOT_MONO, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(1), 50, TOT_MONO, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(2), 75, TOT_MONO, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(3), 100, TOT_MONO, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(4), 125, TOT_MONO, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(5), 150, TOT_MONO, SIMULATION, true),
        entityOutput(DATE_FROM, 13, TOT_MULTI_ORDER, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(1), 26, TOT_MULTI_ORDER, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(2), 39, TOT_MULTI_ORDER, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(3), 52, TOT_MULTI_ORDER, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(4), 65, TOT_MULTI_ORDER, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(5), 78, TOT_MULTI_ORDER, SIMULATION, true),
        entityOutput(DATE_FROM, 10, TOT_MULTI_BATCH, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(1), 20, TOT_MULTI_BATCH, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(2), 30, TOT_MULTI_BATCH, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(3), 40, TOT_MULTI_BATCH, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(4), 50, TOT_MULTI_BATCH, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(5), 60, TOT_MULTI_BATCH, SIMULATION, true)
    );
  }

  private List<ProductivityOutput> mockProductivity() {
    return List.of(
        productivityOutput(DATE_FROM, 25, TOT_MONO, SIMULATION, 1),
        productivityOutput(DATE_FROM, 20, TOT_MONO, FORECAST, 1),
        productivityOutput(DATE_FROM, 35, TOT_MONO, FORECAST, 2),
        productivityOutput(DATE_FROM.plusHours(1), 50, TOT_MONO, SIMULATION, 1),
        productivityOutput(DATE_FROM.plusHours(1), 63, TOT_MONO, FORECAST, 1),
        productivityOutput(DATE_FROM.plusHours(2), 75, TOT_MONO, FORECAST, 1),
        productivityOutput(DATE_FROM.plusHours(3), 100, TOT_MONO, FORECAST, 1),
        productivityOutput(DATE_FROM.plusHours(4), 125, TOT_MONO, FORECAST, 1),
        productivityOutput(DATE_FROM.plusHours(5), 150, TOT_MONO, FORECAST, 1),
        productivityOutput(DATE_FROM, 13, TOT_MULTI_ORDER, FORECAST, 1),
        productivityOutput(DATE_FROM.plusHours(1), 26, TOT_MULTI_ORDER, FORECAST, 1),
        productivityOutput(DATE_FROM.plusHours(1), 36, TOT_MULTI_ORDER, SIMULATION, 1),
        productivityOutput(DATE_FROM.plusHours(2), 39, TOT_MULTI_ORDER, FORECAST, 1),
        productivityOutput(DATE_FROM.plusHours(3), 52, TOT_MULTI_ORDER, FORECAST, 1),
        productivityOutput(DATE_FROM.plusHours(4), 65, TOT_MULTI_ORDER, FORECAST, 1),
        productivityOutput(DATE_FROM.plusHours(4), 68, TOT_MULTI_ORDER, SIMULATION, 1),
        productivityOutput(DATE_FROM.plusHours(5), 78, TOT_MULTI_ORDER, FORECAST, 1)
    );
  }

  private List<EntityOutput> mockHeadcount() {
    return List.of(
        entityOutput(DATE_FROM, 25, TOT_MONO, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(1), 40, TOT_MONO, FORECAST, false),
        entityOutput(DATE_FROM.plusHours(1), 50, TOT_MONO, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(2), 75, TOT_MONO, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(3), 100, TOT_MONO, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(4), 125, TOT_MONO, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(5), 150, TOT_MONO, SIMULATION, false),
        entityOutput(DATE_FROM, 13, TOT_MULTI_ORDER, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(1), 26, TOT_MULTI_ORDER, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(2), 39, TOT_MULTI_ORDER, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(3), 52, TOT_MULTI_ORDER, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(3), 55, TOT_MULTI_ORDER, FORECAST, false),
        entityOutput(DATE_FROM.plusHours(4), 65, TOT_MULTI_ORDER, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(5), 78, TOT_MULTI_ORDER, SIMULATION, false),
        entityOutput(DATE_FROM, 10, TOT_MULTI_BATCH, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(1), 20, TOT_MULTI_BATCH, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(2), 30, TOT_MULTI_BATCH, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(2), 33, TOT_MULTI_BATCH, FORECAST, false),
        entityOutput(DATE_FROM.plusHours(3), 40, TOT_MULTI_BATCH, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(4), 50, TOT_MULTI_BATCH, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(5), 60, TOT_MULTI_BATCH, SIMULATION, false)
    );
  }

}
