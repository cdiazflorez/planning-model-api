package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.GetCapacityPerHourUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.BacklogProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.BacklogProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.GetDeliveryPromiseProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeliveryPromiseProjectionInput;
import com.mercadolibre.planning.model.api.exception.ForecastNotFoundException;
import com.mercadolibre.planning.model.api.web.controller.projection.ProjectionController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.ZonedDateTime;
import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static java.time.ZonedDateTime.now;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProjectionController.class)
public class ProjectionControllerTest {

    private static final String URL = "/planning/model/workflows/{workflow}/projections";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private GetThroughputUseCase getThroughputUseCase;

    @MockBean
    private GetPlanningDistributionUseCase getPlanningUseCase;

    @MockBean
    private CalculateCptProjectionUseCase calculateCptProjection;

    @MockBean
    private CalculateBacklogProjectionUseCase calculateBacklogProjection;

    @MockBean
    private GetCapacityPerHourUseCase getCapacityPerHourUseCase;

    @MockBean
    private GetDeliveryPromiseProjectionUseCase getdevPromiseProjection;

    @Test
    public void testGetCptProjection() throws Exception {
        // GIVEN
        when(getThroughputUseCase.execute(any(GetEntityInput.class)))
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
    public void testGetCptProjectionForecastNotFound() throws Exception {
        // GIVEN
        final ZonedDateTime etd = parse("2020-01-01T11:00:00Z");
        final ZonedDateTime projectedTime = parse("2020-01-02T10:00:00Z");
        final int quantity = 100;

        when(calculateCptProjection.execute(any(CptProjectionInput.class)))
                .thenReturn(List.of(
                        new CptProjectionOutput(etd, projectedTime, quantity)
                ));

        when(getCapacityPerHourUseCase.execute(any(List.class)))
                .thenReturn(List.of(
                        new CapacityOutput(now().withFixedOffsetZone(),
                                UNITS_PER_HOUR,100)
                ));

        // WHEN
        final ResultActions result = mvc.perform(
                post(URL + "/cpts", "fbm-wms-outbound")
                        .contentType(APPLICATION_JSON)
                        .content(getResourceAsString("get_cpt_projection_request.json"))
        );

        // THEN
        verify(getPlanningUseCase).execute(any(GetPlanningDistributionInput.class));
        verify(getThroughputUseCase).execute(any(GetEntityInput.class));
        verifyZeroInteractions(calculateBacklogProjection);

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date")
                        .value(etd.format(ISO_OFFSET_DATE_TIME)))
                .andExpect(jsonPath("$[0].projected_end_date")
                        .value(projectedTime.format(ISO_OFFSET_DATE_TIME)))
                .andExpect(jsonPath("$[0].remaining_quantity")
                        .value(100));
    }

    @Test
    public void testGetCapacityProjection() throws Exception {
        // GIVEN
        final ZonedDateTime etd = parse("2021-01-01T11:00:00Z");
        final ZonedDateTime projectedTime = parse("2021-01-02T10:00:00Z");
        final int quantity = 100;

        when(getdevPromiseProjection.execute(GetDeliveryPromiseProjectionInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(Workflow.FBM_WMS_OUTBOUND)
                .dateFrom(parse("2020-01-01T12:00:00Z[UTC]"))
                .dateTo(parse("2020-01-10T12:00:00Z[UTC]"))
                .backlog(emptyList())
                .build()
        )).thenReturn(List.of(new CptProjectionOutput(etd, projectedTime, quantity)));


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
        verifyZeroInteractions(calculateCptProjection);
        //TODO: Add asserts
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8));
    }
}
