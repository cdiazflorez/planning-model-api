package com.mercadolibre.planning.model.api.web.controller.plan.staffing;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_ORDER;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static com.mercadolibre.planning.model.api.web.controller.plan.staffing.StaffingPlanTestUtils.mockHeadcount;
import static com.mercadolibre.planning.model.api.web.controller.plan.staffing.StaffingPlanTestUtils.mockProductivity;
import static com.mercadolibre.planning.model.api.web.controller.plan.staffing.StaffingPlanTestUtils.mockThroughputs;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.service.lastupdatedentity.LastEntityModifiedDateService;
import com.mercadolibre.planning.model.api.domain.service.lastupdatedentity.LastModifiedDates;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.activate.ActivateSimulationUseCase;
import com.mercadolibre.planning.model.api.exception.ForecastNotFoundException;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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

  private static final String LAST_UPDATED_URL = BASE_URL + "/last_updated";

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

  @MockBean
  private ActivateSimulationUseCase activateSimulationUseCase;

  @MockBean
  private LastEntityModifiedDateService lastEntityModifiedService;

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
    when(activateSimulationUseCase.execute(any())).thenReturn(anyList());

    // WHEN
    final ResultActions result = mvc.perform(
        put(BASE_URL, LOGISTIC_CENTER_ID)
            .param("user_id", "123")
            .param("workflow", "fbm-wms-outbound")
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("controller/plan/staffing/put_plan_staffing_entities_request.json"))

    );

    // THEN
    result.andExpect(status().isOk());
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

  @Test
  void testGetLastModifiedDatesOk() throws Exception {

    when(lastEntityModifiedService.getLastEntityDateModified(anyString(), any(), any(), any()))
        .thenReturn(
            new LastModifiedDates(A_DATE_UTC.toInstant(),
                Map.of(
                    EntityType.HEADCOUNT_SYSTEMIC, A_DATE_UTC.toInstant().plus(1, ChronoUnit.HOURS),
                    EntityType.HEADCOUNT_NON_SYSTEMIC, A_DATE_UTC.toInstant().plus(2, ChronoUnit.HOURS),
                    EntityType.PRODUCTIVITY, A_DATE_UTC.toInstant().plus(3, ChronoUnit.HOURS)
                )
            )
        );

    // WHEN
    final ResultActions result = mvc.perform(
        get(LAST_UPDATED_URL, LOGISTIC_CENTER_ID)
            .contentType(APPLICATION_JSON)
            .param("workflow", "fbm-wms-outbound")
            .param("entity_types", "headcount_systemic,productivity,headcount_non_systemic")
            .param("view_date", "2020-08-20T06:00:00Z")
    );

    result.andExpect(status().isOk())
        .andExpect(content().json(getResourceAsString("controller/plan/staffing/get_last_modified_dates_request.json")));
  }

  @Test
  void testGetLastModifiedDatesError() throws Exception {

    when(lastEntityModifiedService.getLastEntityDateModified(anyString(), any(), any(), any()))
        .thenThrow(ForecastNotFoundException.class);

    // WHEN
    final ResultActions result = mvc.perform(
        get(LAST_UPDATED_URL, LOGISTIC_CENTER_ID)
            .contentType(APPLICATION_JSON)
            .param("workflow", "fbm-wms-outbound")
            .param("view_date", "2020-08-20T06:00:00Z")
    );

    result.andExpect(status().isNotFound());
  }

  @Test
  void testGetLastModifiedDatesBadRequest() throws Exception {

    when(lastEntityModifiedService.getLastEntityDateModified(anyString(), any(), any(), any()))
        .thenReturn(new LastModifiedDates(Instant.now(), Map.of()));

    // WHEN
    final ResultActions result = mvc.perform(
        get(LAST_UPDATED_URL, LOGISTIC_CENTER_ID)
            .contentType(APPLICATION_JSON)
            .param("workflow", "fbmwms-outbound")
    );

    result.andExpect(status().isBadRequest());
  }
}
