package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.ProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.BacklogProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.BacklogProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.DeliveryPromiseProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.GetCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.GetDeliveryPromiseProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetCptProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeliveryPromiseProjectionInput;
import com.mercadolibre.planning.model.api.exception.ForecastNotFoundException;
import com.mercadolibre.planning.model.api.web.controller.projection.ProjectionController;
import com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.ZonedDateTime;
import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProjectionController.class)
class ProjectionControllerTest {

    private static final String URL = "/planning/model/workflows/{workflow}/projections";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CalculateBacklogProjectionUseCase calculateBacklogProjection;

    @MockBean
    private GetDeliveryPromiseProjectionUseCase delPromiseProjection;

    @MockBean
    private GetThroughputUseCase getThroughputUseCase;

    @MockBean
    private GetPlanningDistributionUseCase getPlanningUseCase;

    @MockBean
    private GetCptProjectionUseCase getCptProjectionUseCase;

    @Test
    public void testGetCptProjectionForecastNotFound() throws Exception {
        // GIVEN
        when(getCptProjectionUseCase.execute(any(GetCptProjectionInput.class)))
                .thenThrow(ForecastNotFoundException.class);

        // WHEN
        final ResultActions result = mvc.perform(
                post(URL + "/cpts", "fbm-wms-outbound")
                        .contentType(APPLICATION_JSON)
                        .content(getResourceAsString("get_cpt_projection_request.json"))
        );

        // THEN
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("error").value("forecast_not_found"));
    }

    @Test
    public void testGetCptProjection() throws Exception {
        // GIVEN
        final ZonedDateTime etd = parse("2020-01-01T11:00:00Z");
        final ZonedDateTime projectedTime = parse("2020-01-02T10:00:00Z");

        when(getCptProjectionUseCase.execute(
                new GetCptProjectionInput(
                        Workflow.FBM_WMS_OUTBOUND,
                        WAREHOUSE_ID,
                        ProjectionType.CPT,
                        List.of(PICKING, PACKING),
                        parse("2020-01-01T12:00:00Z[UTC]"),
                        parse("2020-01-10T12:00:00Z[UTC]"),
                        null,
                        "America/Argentina/Buenos_Aires",
                        false
                )))
                .thenReturn(
                        List.of(new CptProjectionOutput(
                                etd, projectedTime, 100
                        )));

        // WHEN
        final ResultActions result = mvc.perform(
                post(URL + "/cpts", "fbm-wms-outbound")
                        .contentType(APPLICATION_JSON)
                        .content(getResourceAsString("get_cpt_projection_request.json"))
        );

        // THEN
        verifyNoInteractions(getPlanningUseCase);
        verifyNoInteractions(getThroughputUseCase);
        verifyNoInteractions(calculateBacklogProjection);

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date")
                        .value(etd.format(ISO_OFFSET_DATE_TIME)))
                .andExpect(jsonPath("$[0].projected_end_date")
                        .value(projectedTime.format(ISO_OFFSET_DATE_TIME)))
                .andExpect(jsonPath("$[0].remaining_quantity")
                        .value(100));
    }

    @Test
    public void testGetDeliveryPromise() throws Exception {
        // GIVEN
        final ZonedDateTime etd = parse("2021-01-01T11:00:00Z");
        final ZonedDateTime projectedTime = parse("2021-01-02T10:00:00Z");
        final ZonedDateTime payBefore = parse("2021-01-01T07:00:00Z");

        when(delPromiseProjection.execute(GetDeliveryPromiseProjectionInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(Workflow.FBM_WMS_OUTBOUND)
                .dateFrom(parse("2020-01-01T12:00:00Z[UTC]"))
                .dateTo(parse("2020-01-10T12:00:00Z[UTC]"))
                .timeZone("America/Argentina/Buenos_Aires")
                .backlog(emptyList())
                .build()
        )).thenReturn(List.of(
                new DeliveryPromiseProjectionOutput(
                        etd,
                        projectedTime,
                        100,
                        null,
                        new ProcessingTime(240L, MINUTES),
                        payBefore,
                        false)));

        // WHEN
        final ResultActions result = mvc.perform(
                post(URL + "/cpts/delivery_promise", "fbm-wms-outbound")
                        .contentType(APPLICATION_JSON)
                        .content(getResourceAsString("get_cpt_projection_request.json"))
        );

        // THEN
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date")
                        .value(etd.format(ISO_OFFSET_DATE_TIME)))
                .andExpect(jsonPath("$[0].projected_end_date")
                        .value(projectedTime.format(ISO_OFFSET_DATE_TIME)))
                .andExpect(jsonPath("$[0].pay_before")
                        .value(payBefore.format(ISO_OFFSET_DATE_TIME)))
                .andExpect(jsonPath("$[0].remaining_quantity")
                        .value(100));
    }

    @Test
    public void testGetBacklogProjection() throws Exception {
        // GIVEN
        when(calculateBacklogProjection.execute(any(BacklogProjectionInput.class)))
                .thenReturn(List.of(
                        BacklogProjectionOutput.builder()
                                .processName(WAVING)
                                .values(emptyList()).build(),
                        BacklogProjectionOutput.builder()
                                .processName(PICKING)
                                .values(emptyList()).build(),
                        BacklogProjectionOutput.builder()
                                .processName(PACKING)
                                .values(emptyList()).build()
                ));

        // WHEN
        final ResultActions result = mvc.perform(
                post(URL + "/backlogs", "fbm-wms-outbound")
                        .contentType(APPLICATION_JSON)
                        .content(getResourceAsString("get_backlog_projection_request.json")));

        // THEN
        //TODO: Add asserts
        result.andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));
    }
}
