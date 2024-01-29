package com.mercadolibre.planning.model.api.web.controller.plan.staffing;

import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.update.UpdateStaffingPlanUseCase;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest;
import java.time.ZonedDateTime;
import java.util.List;
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
  private static final String LOGISTIC_CENTER_ID = "ARTW01";
  private static final ZonedDateTime DATE_FROM = A_DATE_UTC;

  private static final ZonedDateTime DATE_TO = A_DATE_UTC.plusHours(5);


  @Autowired
  private MockMvc mvc;

  @MockBean
  private StaffingPlanAdapter staffingPlanAdapter;

  @MockBean
  private UpdateStaffingPlanUseCase updateStaffingPlanUseCase;

  private static StaffingPlanRequest buildStaffingPlanRequest() {
    return new StaffingPlanRequest(
        List.of(EntityType.HEADCOUNT, EntityType.PRODUCTIVITY, EntityType.THROUGHPUT),
        Workflow.FBM_WMS_OUTBOUND,
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
}
