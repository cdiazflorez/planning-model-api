package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.domain.usecase.GetPlanningDistributionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.CalculateProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.ProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.ProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.strategy.CalculateProjectionStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static com.mercadolibre.planning.model.api.web.controller.request.ProjectionType.BACKLOG;
import static com.mercadolibre.planning.model.api.web.controller.request.ProjectionType.CPT;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
    private CalculateProjectionStrategy calculateProjectionStrategy;

    @MockBean
    private GetThroughputUseCase getThroughputUseCase;

    @MockBean
    private GetPlanningDistributionUseCase getPlanningUseCase;

    @MockBean
    private CalculateProjectionUseCase calculateProjectionUseCase;

    @Test
    public void testGetProjection() throws Exception {
        // GIVEN
        final ZonedDateTime etd = parse("2020-01-01T11:00:00Z");
        final ZonedDateTime projectedTime = parse("2020-01-02T10:00:00Z");
        final int quantity = 100;

        when(calculateProjectionStrategy.getBy(CPT))
                .thenReturn(Optional.of(calculateProjectionUseCase));

        when(calculateProjectionUseCase.execute(any(ProjectionInput.class)))
                .thenReturn(List.of(
                        new ProjectionOutput(etd, projectedTime, quantity)
                ));

        // WHEN
        final ResultActions result = mvc.perform(
                post(URL, "fbm-wms-outbound")
                        .contentType(APPLICATION_JSON)
                        .content(getResourceAsString("get_cpt_projection_request.json"))
        );

        // THEN
        verify(getPlanningUseCase).execute(any(GetPlanningDistributionInput.class));
        verify(getThroughputUseCase).execute(any(GetEntityInput.class));

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
        when(calculateProjectionStrategy.getBy(BACKLOG)).thenReturn(Optional.empty());

        // WHEN
        final ResultActions result = mvc.perform(
                post(URL, "fbm-wms-outbound")
                        .contentType(APPLICATION_JSON)
                        .content(getResourceAsString("get_backlog_projection_request.json")));

        // THEN
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.error").value("projection_type_not_supported"))
                .andExpect(jsonPath("$.message").value("Projection type BACKLOG is not supported"));
    }
}
