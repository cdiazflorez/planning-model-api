package com.mercadolibre.planning.model.api.web.controller.plan.staffing;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS_NS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE;
import static com.mercadolibre.planning.model.api.util.TestUtils.PROCESS_NAME;
import static com.mercadolibre.planning.model.api.util.TestUtils.PROCESS_PATH;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.entity.plan.StaffingPlanResponse;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.plan.staffing.GetStaffingPlanUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.update.UpdateStaffingPlanUseCase;
import com.mercadolibre.planning.model.api.domain.entity.plan.StaffingPlanResponse;
import com.mercadolibre.planning.model.api.domain.usecase.plan.staffing.GetStaffingPlanUseCase;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(StaffingPlanController.class)
public class StaffingPlanControllerTest {
  private static final String BASE_URL = "/logistic_center/{logistic_center_id}/plan/staffing/v2";

  private static final String NEW_URL = "/logistic_center/{logistic_center_id}/plan/staffing/v2/{type}";

  private static final String LOGISTIC_CENTER_ID = "ARTW01";
  private static final ZonedDateTime DATE_FROM = A_DATE_UTC;

  private static final ZonedDateTime DATE_TO = A_DATE_UTC.plusHours(5);


  @Autowired
  private MockMvc mvc;

  @MockBean
  private StaffingPlanAdapter staffingPlanAdapter;

  @MockBean
  private UpdateStaffingPlanUseCase updateStaffingPlanUseCase;

  @MockBean
  private GetStaffingPlanUseCase getStaffingPlanUseCase;

  private static StaffingPlanRequest buildStaffingPlanRequest() {
    return new StaffingPlanRequest(
        List.of(EntityType.HEADCOUNT, EntityType.PRODUCTIVITY, EntityType.THROUGHPUT),
        FBM_WMS_OUTBOUND,
        DATE_FROM.withFixedOffsetZone(),
        DATE_TO.withFixedOffsetZone(),
        A_DATE_UTC.toInstant(),
        LOGISTIC_CENTER_ID,
        List.of(
            StaffingPlanRequest.Groupers.PROCESS_PATH,
            StaffingPlanRequest.Groupers.PROCESS_NAME,
            StaffingPlanRequest.Groupers.DATE
        ),
        List.of(),
        List.of(),
        List.of(),
        List.of()
    );
  }

  @Test
  @DisplayName("Get resources ok")
  void testGetResourcesOk() throws Exception {
    when(staffingPlanAdapter.getStaffingPlan(buildStaffingPlanRequest()))
        .thenReturn(any());

    final ResultActions result = mvc.perform(
        get(BASE_URL, LOGISTIC_CENTER_ID)
            .contentType(APPLICATION_JSON)
            .param("workflow", "fbm-wms-outbound")
            .param("date_from", DATE_FROM.toInstant().toString())
            .param("date_to", DATE_TO.toInstant().toString())
            .param("view_date", DATE_FROM.toInstant().toString())
            .param("resources", "headcount,productivity,throughput")
            .param("groupers", "process_path,process_name,date")
    );
    result.andExpect(status().isOk());
  }

  @Test
  @DisplayName("Get resources with invalid groupers")
  void testInvalidGroups() throws Exception {

    final ResultActions result = mvc.perform(
        get(BASE_URL, LOGISTIC_CENTER_ID)
            .contentType(APPLICATION_JSON)
            .param("workflow", "fbm-wms-outbound")
            .param("date_from", DATE_FROM.toInstant().toString())
            .param("date_to", DATE_TO.toInstant().toString())
            .param("view_date", DATE_FROM.toInstant().toString())
            .param("resources", "headcount,productivity,throughput")
            .param("groupers", "invalid_grouper")
    );
    result.andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Update staffing plan ok")
  void testUpdateStaffingPlanOk() throws Exception {
    doNothing().when(updateStaffingPlanUseCase).execute(any());

    //WHEN
    final ResultActions result = mvc.perform(
        put(BASE_URL, LOGISTIC_CENTER_ID)
            .param("user_id", "123")
            .param("workflow", "fbm-wms-outbound")
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("controller/plan/staffing/put_staffing_plan_new_request.json"))
    );

    //THEN
    result.andExpect(status().isOk());
  }

  @Test
  @DisplayName("Get staffing plan headcount")
  void testGetStaffingPlanHeadcount() throws Exception {

    when(getStaffingPlanUseCase.execute(
             LOGISTIC_CENTER_ID,
             FBM_WMS_OUTBOUND,
             EFFECTIVE_WORKERS,
             List.of(DATE),
             emptyMap(),
             DATE_FROM.toInstant(),
             DATE_TO.toInstant(),
             DATE_FROM.toInstant()
         )
    ).thenReturn(
        List.of(
            new StaffingPlanResponse(99, 99, Map.of(DATE, DATE_FROM.toInstant().toString())
            )
        )
    );

    when(getStaffingPlanUseCase.execute(
             LOGISTIC_CENTER_ID,
             FBM_WMS_OUTBOUND,
             EFFECTIVE_WORKERS_NS,
             List.of(DATE),
             emptyMap(),
             DATE_FROM.toInstant(),
             DATE_TO.toInstant(),
             DATE_FROM.toInstant()
         )
    ).thenReturn(
        List.of(
            new StaffingPlanResponse(1, 1, Map.of(DATE, DATE_FROM.toInstant().toString())
            )
        )
    );

    final ResultActions result = mvc.perform(
        get(NEW_URL, LOGISTIC_CENTER_ID, HEADCOUNT.toJson())
            .contentType(APPLICATION_JSON)
            .param("workflow", "fbm-wms-outbound")
            .param("date_from", DATE_FROM.toInstant().toString())
            .param("date_to", DATE_TO.toInstant().toString())
            .param("view_date", DATE_FROM.toInstant().toString())
            .param("groupers", "date")
    );

    result.andExpect(status().isOk())
        .andExpect(content().json(getResourceAsString("get_staffing_plan_headcount.json")));

  }

  @Test
  @DisplayName("Get staffing plan productivity")
  void testGetStaffingPlanProductivity() throws Exception {

    when(getStaffingPlanUseCase.execute(
             LOGISTIC_CENTER_ID,
             FBM_WMS_OUTBOUND,
             PRODUCTIVITY,
             List.of(DATE, PROCESS_NAME, PROCESS_PATH),
             Map.of(
                 PROCESS_NAME, List.of(PICKING.toJson()),
                 PROCESS_PATH, List.of(GLOBAL.toJson())
             ),
             DATE_FROM.toInstant(),
             DATE_TO.toInstant(),
             DATE_FROM.toInstant()
         )
    ).thenReturn(
        List.of(
            new StaffingPlanResponse(
                250,
                250,
                Map.of(
                    DATE, DATE_FROM.toInstant().toString(),
                    PROCESS_NAME, PICKING.toJson(),
                    PROCESS_PATH, GLOBAL.toJson()
                )
            ),
            new StaffingPlanResponse(
                300,
                300,
                Map.of(
                    DATE, DATE_TO.toInstant().toString(),
                    PROCESS_NAME, PICKING.toJson(),
                    PROCESS_PATH, GLOBAL.toJson()
                )
            )
        )
    );

    final ResultActions result = mvc.perform(
        get(NEW_URL, LOGISTIC_CENTER_ID, PRODUCTIVITY.toJson())
            .contentType(APPLICATION_JSON)
            .param("workflow", "fbm-wms-outbound")
            .param("date_from", DATE_FROM.toInstant().toString())
            .param("date_to", DATE_TO.toInstant().toString())
            .param("view_date", DATE_FROM.toInstant().toString())
            .param("groupers", "date,process_name,process_path")
            .param("process_name", "picking")
            .param("process_path", "global")
    );

    result.andExpect(status().isOk())
        .andExpect(content().json(getResourceAsString("get_staffing_plan_productivity.json")));

  }
}
