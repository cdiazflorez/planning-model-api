package com.mercadolibre.planning.model.api.web.controller;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionService;
import com.mercadolibre.planning.model.api.web.controller.forecast.PlannedUnitsController;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = PlannedUnitsController.class)
public class PlannedUnitsControllerTest {

    private static final String URL = "/logistic_center/{logistic_center}/plan/units";
    private static final String WH = "ARBA01";
    private static final String DATE_IN_FROM = "2022-09-10T10:00:00Z";
    private static final String DATE_IN_TO = "2022-09-10T14:00:00Z";
    private static final String DATE_OUT_FROM = "2022-09-11T10:00:00Z";
    private static final String DATE_OUT_TO = "2022-09-11T14:00:00Z";
    private static final String WORKFLOW = "fbm-wms-outbound";
    private static final String PROCESS_PATHS = "tot_mono,non_tot_mono";
    private static final String GROUP_BY_ALL = "process_path,date_in,date_out";
    private static final String GROUP_BY_DATE_IN = "date_in";
    private static final String GROUP_BY_DATE_OUT = "date_out";
    private static final String GROUP_BY_DATE_OUT_AND_PROCESS_PATH = "process_path,date_out";

    private static final String DATE_IN_FROM_PARAM = "date_in_from";
    private static final String DATE_IN_TO_PARAM = "date_in_to";
    private static final String DATE_OUT_FROM_PARAM = "date_out_from";
    private static final String DATE_OUT_TO_PARAM = "date_out_to";
    private static final String WORKFLOW_PARAM = "workflow";
    private static final String PROCESS_PATHS_PARAM = "process_paths";
    private static final String GROUP_BY_PARAM = "group_by";
    private static final String VIEW_DATE_PARAM = "view_date";

    @MockBean
    private PlanningDistributionService planningDistributionService;

    @Autowired
    private MockMvc mvc;

    @ParameterizedTest
    @MethodSource("provideArgumentsAndExpectedValueInPlannedUnits")
    public void testGetForecast(final String groupBy, final String expectedValue) throws Exception {

        //GIVEN
        when(planningDistributionService.getPlanningDistribution(GetPlanningDistributionInput.builder()
                .warehouseId(WH)
                .workflow(Workflow.FBM_WMS_OUTBOUND)
                .dateOutFrom(Instant.parse(DATE_OUT_FROM))
                .dateOutTo(Instant.parse(DATE_OUT_TO))
                .dateInFrom(Instant.parse(DATE_IN_FROM))
                .dateInTo(Instant.parse(DATE_IN_TO))
                .processPaths(Set.of(TOT_MONO, NON_TOT_MONO))
                .applyDeviation(true)
                .viewDate(Instant.parse(DATE_IN_FROM))
                .build()))
                .thenReturn(mockGetPlanningDistributionOutput());

        // WHEN
        final ResultActions result = mvc.perform(
                get(URL, WH)
                        .contentType(APPLICATION_JSON)
                        .param(WORKFLOW_PARAM, WORKFLOW)
                        .param(PROCESS_PATHS_PARAM, PROCESS_PATHS)
                        .param(DATE_IN_FROM_PARAM, DATE_IN_FROM)
                        .param(DATE_IN_TO_PARAM, DATE_IN_TO)
                        .param(DATE_OUT_FROM_PARAM, DATE_OUT_FROM)
                        .param(DATE_OUT_TO_PARAM, DATE_OUT_TO)
                        .param(VIEW_DATE_PARAM, DATE_IN_FROM)
                        .param(GROUP_BY_PARAM, groupBy)
        );

        //THEN
        result.andExpect(status().isOk())
                .andExpect(content().json(getResourceAsString(expectedValue)));

    }

    @Test
    public void testGetForecastError() throws Exception {

        // WHEN
        final ResultActions result = mvc.perform(
                get(URL, WH)
                        .contentType(APPLICATION_JSON)
                        .param(WORKFLOW_PARAM, WORKFLOW)
                        .param(PROCESS_PATHS_PARAM, PROCESS_PATHS)
                        .param(DATE_IN_FROM_PARAM, DATE_IN_FROM)
                        .param(DATE_OUT_TO_PARAM, DATE_OUT_TO)
                        .param(VIEW_DATE_PARAM, DATE_IN_FROM)
                        .param(GROUP_BY_PARAM, GROUP_BY_ALL)
        );

        // WHEN
        final ResultActions resultTwo = mvc.perform(
                get(URL, WH)
                        .contentType(APPLICATION_JSON)
                        .param(WORKFLOW_PARAM, WORKFLOW)
                        .param(PROCESS_PATHS_PARAM, PROCESS_PATHS)
                        .param(VIEW_DATE_PARAM, DATE_IN_FROM)
                        .param(GROUP_BY_PARAM, GROUP_BY_ALL)
        );

        // WHEN
        final ResultActions resultThree = mvc.perform(
                get(URL, WH)
                        .contentType(APPLICATION_JSON)
                        .param(WORKFLOW_PARAM, WORKFLOW)
                        .param(PROCESS_PATHS_PARAM, PROCESS_PATHS)
                        .param(DATE_IN_TO_PARAM, DATE_IN_TO)
                        .param(DATE_OUT_FROM_PARAM, DATE_OUT_FROM)
                        .param(VIEW_DATE_PARAM, DATE_IN_FROM)
                        .param(GROUP_BY_PARAM, GROUP_BY_ALL)
        );

        result.andExpect(status().is4xxClientError());
        resultTwo.andExpect(status().is4xxClientError());
        resultThree.andExpect(status().is4xxClientError());

    }

    private static Stream<Arguments> provideArgumentsAndExpectedValueInPlannedUnits() {
        return Stream.of(
                Arguments.of(
                        GROUP_BY_ALL,
                        "controller/forecast/planned_units_response_all_groups.json"
                ),
                Arguments.of(
                        GROUP_BY_DATE_IN,
                        "controller/forecast/planned_units_response_group_by_date_in.json"
                ),
                Arguments.of(
                        GROUP_BY_DATE_OUT,
                        "controller/forecast/planned_units_response_group_by_date_out.json"
                ),
                Arguments.of(
                        GROUP_BY_DATE_OUT_AND_PROCESS_PATH,
                        "controller/forecast/planned_units_response_group_by_date_out_and_process_path.json"
                )
        );
    }

    private static List<GetPlanningDistributionOutput> mockGetPlanningDistributionOutput() {
        return List.of(
                new GetPlanningDistributionOutput(Instant.parse(DATE_IN_FROM),
                        Instant.parse(DATE_OUT_FROM),
                        UNITS,
                        TOT_MONO,
                        10.5D),
                new GetPlanningDistributionOutput(Instant.parse(DATE_IN_FROM),
                        Instant.parse(DATE_OUT_FROM),
                        UNITS,
                        NON_TOT_MONO,
                        12.0D),
                new GetPlanningDistributionOutput(Instant.parse(DATE_IN_TO),
                        Instant.parse(DATE_OUT_TO),
                        UNITS,
                        TOT_MONO,
                        20.6D),
                new GetPlanningDistributionOutput(Instant.parse(DATE_IN_TO),
                        Instant.parse(DATE_OUT_TO),
                        UNITS,
                        NON_TOT_MONO,
                        4.4D)
        );
    }
}
