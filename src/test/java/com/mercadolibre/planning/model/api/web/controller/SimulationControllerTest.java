package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.GetCapacityPerHourUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.ProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.activate.ActivateSimulationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.activate.SimulationInput;
import com.mercadolibre.planning.model.api.web.controller.simulation.SimulationController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static java.time.ZonedDateTime.now;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SimulationController.class)
public class SimulationControllerTest {

    private static final String URL = "/planning/model/workflows/{workflow}/simulations";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private GetThroughputUseCase getForecastedThroughputUseCase;

    @MockBean
    private GetPlanningDistributionUseCase getPlanningDistributionUseCase;

    @MockBean
    private CalculateCptProjectionUseCase calculateCptProjectionUseCase;

    @MockBean
    private ActivateSimulationUseCase activateSimulationUseCase;

    @MockBean
    private GetCapacityPerHourUseCase getCapacityPerHourUseCase;

    @MockBean
    private GetCycleTimeUseCase getCycleTimeUseCase;

    @Test
    public void testSaveSimulation() throws Exception {
        // GIVEN
        final ZonedDateTime dateOut = parse("2020-01-01T10:00:00Z");
        final ZonedDateTime projectedEndDate = parse("2020-01-01T11:00:00Z");
        final Map<ZonedDateTime, Configuration> ctByDateOut = new HashMap<>();
        ctByDateOut.put(dateOut, Configuration.builder()
                .value(360L)
                .metricUnit(MINUTES)
                .key("processing_time")
                .build());

        when(calculateCptProjectionUseCase.execute(any(CptProjectionInput.class)))
                .thenReturn(List.of(
                        CptProjectionOutput.builder()
                                .date(dateOut)
                                .projectedEndDate(projectedEndDate)
                                .remainingQuantity(100)
                                .processingTime(new ProcessingTime(240L, MINUTES))
                                .isDeferred(false)
                                .build()));

        when(getCapacityPerHourUseCase.execute(any(List.class)))
                .thenReturn(List.of(
                        new CapacityOutput(now().withFixedOffsetZone(),
                                UNITS_PER_HOUR, 100)
                ));

        when(getCycleTimeUseCase.execute(new GetCycleTimeInput("ARBA01",
                List.of(dateOut)))).thenReturn(ctByDateOut);

        // WHEN
        final ResultActions result = mvc.perform(
                post(URL + "/save", "fbm-wms-outbound")
                        .contentType(APPLICATION_JSON)
                        .content(getResourceAsString("simulation_request.json"))
        );

        // THEN
        verify(getForecastedThroughputUseCase).execute(any(GetEntityInput.class));
        verify(getPlanningDistributionUseCase).execute(any(GetPlanningDistributionInput.class));
        verify(activateSimulationUseCase).execute(any(SimulationInput.class));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date")
                        .value(dateOut.format(ISO_OFFSET_DATE_TIME)))
                .andExpect(jsonPath("$[0].projected_end_date")
                        .value(projectedEndDate.format(ISO_OFFSET_DATE_TIME)))
                .andExpect(jsonPath("$[0].remaining_quantity")
                        .value(100))
                .andExpect(jsonPath("$[0].processing_time.value")
                        .value(240))
                .andExpect(jsonPath("$[0].processing_time.unit_metric")
                        .value(MINUTES.toJson()))
                .andExpect(jsonPath("$[0].deferred")
                        .value(false));
    }

    @Test
    public void testRunSimulation() throws Exception {
        // GIVEN
        final ZonedDateTime dateOut = parse("2020-01-01T10:00:00Z");
        final ZonedDateTime simulatedEndDate = parse("2020-01-01T11:00:00Z");
        final ZonedDateTime projectedEndDate = parse("2020-01-01T13:00:00Z");

        final Map<ZonedDateTime, Configuration> ctByDateOut = new HashMap<>();
        ctByDateOut.put(dateOut, Configuration.builder()
                .value(360L)
                .metricUnit(MINUTES)
                .key("processing_time")
                .build());

        when(calculateCptProjectionUseCase.execute(any(CptProjectionInput.class)))
                .thenReturn(List.of(
                        CptProjectionOutput.builder()
                                .date(dateOut)
                                .projectedEndDate(simulatedEndDate)
                                .remainingQuantity(100)
                                .processingTime(new ProcessingTime(240L, MINUTES))
                                .isDeferred(false)
                                .build()
                )).thenReturn(List.of(
                        CptProjectionOutput.builder()
                                .date(dateOut)
                                .projectedEndDate(projectedEndDate)
                                .remainingQuantity(150)
                                .processingTime(new ProcessingTime(240L, MINUTES))
                                .isDeferred(false)
                                .build()
                ));

        when(getCapacityPerHourUseCase.execute(any(List.class)))
                .thenReturn(List.of(
                        new CapacityOutput(now().withFixedOffsetZone(),
                                UNITS_PER_HOUR, 100)
                ));

        when(getCycleTimeUseCase.execute(new GetCycleTimeInput("ARBA01",
                List.of(dateOut)))).thenReturn(ctByDateOut);

        // WHEN
        final ResultActions result = mvc.perform(
                post(URL + "/run", "fbm-wms-outbound")
                        .contentType(APPLICATION_JSON)
                        .content(getResourceAsString("simulation_request.json"))
        );

        // THEN
        //verify(getSimulationThroughputUseCase).execute(any(GetEntityInput.class));
        verify(getPlanningDistributionUseCase).execute(any(GetPlanningDistributionInput.class));
        verify(getForecastedThroughputUseCase, times(2)).execute(any(GetEntityInput.class));
        verifyZeroInteractions(activateSimulationUseCase);

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date")
                        .value(dateOut.format(ISO_OFFSET_DATE_TIME)))
                .andExpect(jsonPath("$[0].projected_end_date")
                        .value(projectedEndDate.format(ISO_OFFSET_DATE_TIME)))
                .andExpect(jsonPath("$[0].simulated_end_date")
                        .value(simulatedEndDate.format(ISO_OFFSET_DATE_TIME)))
                .andExpect(jsonPath("$[0].remaining_quantity")
                        .value(100))
                .andExpect(jsonPath("$[0].processing_time.value")
                        .value(240))
                .andExpect(jsonPath("$[0].processing_time.unit_metric")
                        .value(MINUTES.toJson()))
                .andExpect(jsonPath("$[0].deferred")
                        .value(false));
    }
}
