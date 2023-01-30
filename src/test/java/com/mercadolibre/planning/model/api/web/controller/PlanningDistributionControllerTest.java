package com.mercadolibre.planning.model.api.web.controller;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetPlanningDistOutput;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedBacklogService;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedUnits;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionService;
import com.mercadolibre.planning.model.api.exception.ForecastNotFoundException;
import com.mercadolibre.planning.model.api.web.controller.planningdistribution.PlanningDistributionController;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@WebMvcTest(controllers = PlanningDistributionController.class)
public class PlanningDistributionControllerTest {

  private static final String URL_OUTBOUND = "/planning/model/workflows/fbm-wms-outbound/planning_distributions";

  private static final String URL_INBOUND = "/planning/model/workflows/fbm-wms-inbound/planning_distributions";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private PlanningDistributionService planningDistributionService;

  @MockBean
  private PlannedBacklogService plannedBacklogService;

  private static Stream<Arguments> getParameters() {
    final LinkedMultiValueMap<String, String> withDateInTo = new LinkedMultiValueMap<>();
    withDateInTo.add("warehouse_id", WAREHOUSE_ID);
    withDateInTo.add("date_out_from", A_DATE_UTC.toString());
    withDateInTo.add("date_out_to", A_DATE_UTC.plusDays(2).toString());
    withDateInTo.add("date_in_to", A_DATE_UTC.minusDays(2).toString());
    withDateInTo.add("view_date", A_DATE_UTC.plusDays(3).toInstant().toString());

    final LinkedMultiValueMap<String, String> withoutDateInTo = new LinkedMultiValueMap<>();
    withoutDateInTo.add("warehouse_id", WAREHOUSE_ID);
    withoutDateInTo.add("date_out_from", A_DATE_UTC.toString());
    withoutDateInTo.add("date_out_to", A_DATE_UTC.plusDays(2).toString());

    return Stream.of(Arguments.of(withDateInTo), Arguments.of(withoutDateInTo));
  }

  @DisplayName("Get planning distribution works ok")
  @ParameterizedTest
  @MethodSource("getParameters")
  public void testGetPlanningDistributionOk(final MultiValueMap<String, String> params) throws Exception {
    // GIVEN
    when(planningDistributionService.getPlanningDistribution(any(GetPlanningDistributionInput.class)))
        .thenReturn(mockGetPlanningDistOutput());

    // WHEN
    final ResultActions result = mvc.perform(
        get(URL_OUTBOUND)
            .contentType(APPLICATION_JSON)
            .params(params)
    );

    // THEN
    result.andExpect(status().isOk())
        .andExpect(content().json(getResourceAsString("get_processing_distribution.json")));
  }

  @Test
  void testGetPlanningDistributionWithViewDateOk() throws Exception {
    // GIVEN
    final var dateOutTo = A_DATE_UTC.plusDays(2);
    final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("warehouse_id", WAREHOUSE_ID);
    params.add("date_out_from", A_DATE_UTC.toString());
    params.add("date_out_to", dateOutTo.toString());
    params.add("view_date", dateOutTo.toInstant().toString());

    when(planningDistributionService.getPlanningDistribution(
        GetPlanningDistributionInput.builder()
            .warehouseId(WAREHOUSE_ID)
            .workflow(FBM_WMS_OUTBOUND)
            .dateOutFrom(A_DATE_UTC)
            .dateOutTo(dateOutTo)
            .viewDate(dateOutTo.toInstant())
            .build()
    ))
        .thenReturn(mockGetPlanningDistOutput());

    // WHEN
    final ResultActions result = mvc.perform(
        get(URL_OUTBOUND)
            .contentType(APPLICATION_JSON)
            .params(params)
    );

    // THEN
    result.andExpect(status().isOk())
        .andExpect(content().json(getResourceAsString("get_processing_distribution.json")));
  }

  @DisplayName("Planning distribution returns 404 when forecast doesn't exists")
  @ParameterizedTest
  @MethodSource("getParameters")
  public void testGetPlanningDistributionForecastNotFound(
      final MultiValueMap<String, String> params) throws Exception {
    // GIVEN
    when(planningDistributionService.getPlanningDistribution(any(GetPlanningDistributionInput.class)))
        .thenThrow(ForecastNotFoundException.class);

    // WHEN
    final ResultActions result = mvc.perform(
        get(URL_OUTBOUND)
            .contentType(APPLICATION_JSON)
            .params(params)
    );

    // THEN
    result.andExpect(status().isNotFound())
        .andExpect(jsonPath("error").value("forecast_not_found"));
  }

  @DisplayName("Planning distribution inbound ok")
  @Test
  void testGetPlanningDistributionInbound() throws Exception {

    var dateOutTo = A_DATE_UTC.plusDays(2);

    var viewDate = ZonedDateTime.ofInstant(A_DATE_UTC.plusDays(3).toInstant(), ZoneId.of("UTC"));

    final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("warehouse_id", WAREHOUSE_ID);
    params.add("date_out_from", A_DATE_UTC.toString());
    params.add("date_out_to", dateOutTo.toString());
    params.add("view_date", viewDate.toInstant().toString());
    params.add("apply_deviation", "true");

    // GIVEN
    when(plannedBacklogService.getExpectedBacklog(
            WAREHOUSE_ID,
            FBM_WMS_INBOUND,
            A_DATE_UTC,
            A_DATE_UTC.plusDays(2),
            viewDate,
            true
        )
    ).thenReturn(List.of(
        new PlannedUnits(A_DATE_UTC, A_DATE_UTC.plusDays(1), 1000),
        new PlannedUnits(A_DATE_UTC, A_DATE_UTC.plusDays(1), 2000),
        new PlannedUnits(A_DATE_UTC, A_DATE_UTC.plusDays(2), 500)
    ));

    // WHEN
    final ResultActions result = mvc.perform(
        get(URL_INBOUND)
            .contentType(APPLICATION_JSON)
            .params(params)
    );

    // THEN
    result.andExpect(status().isOk())
        .andExpect(content().json(getResourceAsString("get_planning_distribution_inbound.json")));
  }
}
