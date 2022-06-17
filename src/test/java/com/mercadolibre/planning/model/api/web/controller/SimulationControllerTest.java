package com.mercadolibre.planning.model.api.web.controller;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.entity.sla.ProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.DeliveryPromiseProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.GetSlaProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.SimulationProjectionService;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeliveryPromiseProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetSlaProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.activate.ActivateSimulationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.activate.SimulationInput;
import com.mercadolibre.planning.model.api.web.controller.simulation.SimulationController;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = SimulationController.class)
@SuppressWarnings("PMD.LongVariable")
public class SimulationControllerTest {

  private static final String URL = "/planning/model/workflows/{workflow}/simulations";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private GetSlaProjectionUseCase getSlaProjectionUseCase;

  @MockBean
  private ActivateSimulationUseCase activateSimulationUseCase;

  @MockBean
  private SimulationProjectionService simulationProjectionService;

  @Test
  public void testSaveSimulation() throws Exception {
    // GIVEN
    final ZonedDateTime dateOut = parse("2020-01-01T10:00:00Z");
    final ZonedDateTime projectedEndDate = parse("2020-01-01T11:00:00Z");

    when(getSlaProjectionUseCase.execute(any(GetSlaProjectionInput.class)))
        .thenReturn(List.of(
            new CptProjectionOutput(dateOut, projectedEndDate, 100, null)));

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL + "/save", "fbm-wms-outbound")
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("simulation_request.json"))
    );

    // THEN
    verify(activateSimulationUseCase).execute(any(SimulationInput.class));

    result.andExpect(status().isOk())
        .andExpect(jsonPath("$[0].date")
            .value(dateOut.format(ISO_OFFSET_DATE_TIME)))
        .andExpect(jsonPath("$[0].projected_end_date")
            .value(projectedEndDate.format(ISO_OFFSET_DATE_TIME)))
        .andExpect(jsonPath("$[0].remaining_quantity")
            .value(100));
  }

  @Test
  public void testRunSimulation() throws Exception {
    // GIVEN
    final ZonedDateTime dateOut = parse("2020-01-01T10:00:00Z");
    final ZonedDateTime simulatedEndDate = parse("2020-01-01T11:00:00Z");
    final ZonedDateTime projectedEndDate = parse("2020-01-01T13:00:00Z");

    when(getSlaProjectionUseCase.execute(any(GetSlaProjectionInput.class)))
        .thenReturn(List.of(
            new CptProjectionOutput(dateOut, simulatedEndDate, 100, null)))
        .thenReturn(List.of(
            new CptProjectionOutput(dateOut, projectedEndDate, 150, null)));

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL + "/run", "fbm-wms-outbound")
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("simulation_request.json"))
    );

    // THEN
    verifyNoInteractions(activateSimulationUseCase);

    result.andExpect(status().isOk())
        .andExpect(jsonPath("$[0].date")
            .value(dateOut.format(ISO_OFFSET_DATE_TIME)))
        .andExpect(jsonPath("$[0].projected_end_date")
            .value(projectedEndDate.format(ISO_OFFSET_DATE_TIME)))
        .andExpect(jsonPath("$[0].simulated_end_date")
            .value(simulatedEndDate.format(ISO_OFFSET_DATE_TIME)))
        .andExpect(jsonPath("$[0].remaining_quantity")
            .value(100));
  }

  @Test
  public void testSimulationDeliveryPromise() throws Exception {
    // GIVEN
    final ZonedDateTime etd = parse("2021-01-01T11:00:00Z");
    final ZonedDateTime projectedTime = parse("2021-01-02T10:00:00Z");
    final ZonedDateTime payBefore = parse("2021-01-01T07:00:00Z");

    when(simulationProjectionService.execute(any(GetDeliveryPromiseProjectionInput.class)
    )).thenReturn(List.of(
        new DeliveryPromiseProjectionOutput(
            etd,
            projectedTime,
            100,
            null,
            new ProcessingTime(240L, MINUTES),
            payBefore,
            false))
    );

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL + "/deferral/delivery_promise", "fbm-wms-outbound")
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("get_simulation_deferral_projection_request.json"))
    );

    // THEN
    String response = result.andReturn().getResponse().getContentAsString();
    result.andExpect(status().isOk())
        .andExpect(jsonPath("$[0].date")
            .value(etd.format(ISO_OFFSET_DATE_TIME)))
        .andExpect(jsonPath("$[0].projected_end_date")
            .value(projectedTime.format(ISO_OFFSET_DATE_TIME)))
        .andExpect(jsonPath("$[0].pay_before")
            .value(payBefore.format(ISO_OFFSET_DATE_TIME)))
        .andExpect(jsonPath("$[0].remaining_quantity")
            .value(100));
    assertNotNull(response);

  }
}
