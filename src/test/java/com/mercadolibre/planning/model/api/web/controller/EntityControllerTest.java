package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetRemainingProcessingUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.input.GetHeadcountInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.input.GetProductivityInput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetRemainingProcessingOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockHeadcountEntityOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockProductivityEntityOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockThroughputEntityOutput;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EntityController.class)
public class EntityControllerTest {

    private static final String URL = "/planning/model/workflows/{workflow}/entities/{entity}";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private GetHeadcountEntityUseCase getHeadcountEntityUseCase;

    @MockBean
    private GetProductivityEntityUseCase getProductivityEntityUseCase;

    @MockBean
    private GetThroughputUseCase getThroughputUseCase;
    
    @MockBean
    private GetRemainingProcessingUseCase getRemainingProcessingUseCase;

    @DisplayName("Get headcount entity works ok")
    @Test
    public void testGetHeadcountEntityOk() throws Exception {
        // GIVEN
        when(getHeadcountEntityUseCase.execute(any(GetHeadcountInput.class)))
                .thenReturn(mockHeadcountEntityOutput());

        // WHEN
        final ResultActions result = mvc.perform(
                get(URL, "fbm-wms-outbound", "headcount")
                        .contentType(APPLICATION_JSON)
                        .param("warehouse_id", "ARBA01")
                        .param("source", "forecast")
                        .param("process_name", "picking,packing")
                        .param("processing_type", "active_workers,workers")
                        .param("date_from", A_DATE_UTC.toString())
                        .param("date_to", A_DATE_UTC.plusDays(2).toString())
        );

        // THEN
        verifyZeroInteractions(getProductivityEntityUseCase);
        verifyZeroInteractions(getThroughputUseCase);
        result.andExpect(status().isOk())
                .andExpect(content().json(getResourceAsString("get_headcount_response.json")));
    }

    @DisplayName("Get entity throws InvalidEntityTypeException")
    @Test
    public void testEntityThrowException() throws Exception {
        // WHEN
        final ResultActions result = mvc.perform(
                get(URL, "fbm-wms-outbound", "unknown")
                        .contentType(APPLICATION_JSON)
                        .param("warehouse_id", "ARBA01")
                        .param("source", "forecast")
                        .param("process_name", "picking,packing")
                        .param("date_from", A_DATE_UTC.toString())
                        .param("date_to", A_DATE_UTC.plusDays(2).toString())
        );

        // THEN
        verifyZeroInteractions(getProductivityEntityUseCase);
        result.andExpect(status().isNotFound());
    }

    @DisplayName("Get productivity entity works ok")
    @Test
    public void testGetProductivityEntityOk() throws Exception {
        // GIVEN
        when(getProductivityEntityUseCase.execute(any(GetProductivityInput.class)))
                .thenReturn(mockProductivityEntityOutput());

        // WHEN
        final ResultActions result = mvc.perform(
                get(URL, "fbm-wms-outbound", "productivity")
                        .contentType(APPLICATION_JSON)
                        .param("warehouse_id", "ARBA01")
                        .param("source", "forecast")
                        .param("date_from", A_DATE_UTC.toString())
                        .param("date_to", A_DATE_UTC.plusHours(1).toString())
                        .param("process_name", "picking,packing")
        );

        // THEN
        verifyZeroInteractions(getHeadcountEntityUseCase);
        verifyZeroInteractions(getThroughputUseCase);
        result.andExpect(status().isOk())
                .andExpect(content().json(getResourceAsString("get_productivity_response.json")));
    }

    @DisplayName("Get throughput entity works ok")
    @Test
    public void testGetThroughputEntityOk() throws Exception {
        // GIVEN
        when(getThroughputUseCase.execute(any(GetEntityInput.class)))
                .thenReturn(mockThroughputEntityOutput());

        // WHEN
        final ResultActions result = mvc.perform(
                get(URL, "fbm-wms-outbound", "throughput")
                        .contentType(APPLICATION_JSON)
                        .param("warehouse_id", "ARBA01")
                        .param("source", "forecast")
                        .param("date_from", A_DATE_UTC.toString())
                        .param("date_to", A_DATE_UTC.plusHours(1).toString())
                        .param("process_name", "picking,packing")
        );

        // THEN
        verifyZeroInteractions(getHeadcountEntityUseCase);
        verifyZeroInteractions(getProductivityEntityUseCase);
        result.andExpect(status().isOk())
                .andExpect(content().json(getResourceAsString("get_throughput_response.json")));
    }

    @DisplayName("Get headcount with simulations works ok")
    @Test
    public void testGetHeadcountWithSimulationsOk() throws Exception {
        // GIVEN
        when(getHeadcountEntityUseCase.execute(any(GetHeadcountInput.class)))
                .thenReturn(mockHeadcountEntityOutput());

        // WHEN
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post(URL, "fbm-wms-outbound", "headcount")
                        .contentType(APPLICATION_JSON)
                        .content(getResourceAsString("post_headcount_request.json"))
        );

        // THEN
        verifyZeroInteractions(getProductivityEntityUseCase);
        verifyZeroInteractions(getThroughputUseCase);
        verifyZeroInteractions(getRemainingProcessingUseCase);
        result.andExpect(status().isOk())
                .andExpect(content().json(getResourceAsString("get_headcount_response.json")));
    }
    
    @DisplayName("Get Remaining_processing entity works ok")
    @Test
    public void testGetRemainingProcessingEntityOk() throws Exception {
        // GIVEN
        when(getRemainingProcessingUseCase.execute(any(GetEntityInput.class)))
                .thenReturn(mockGetRemainingProcessingOutput());

        // WHEN
        final ResultActions result = mvc.perform(
                get(URL, "fbm-wms-outbound", "remaining_processing")
                        .contentType(APPLICATION_JSON)
                        .param("warehouse_id", "ARBA01")
                        .param("source", "forecast")
                        .param("date_from", A_DATE_UTC.toString())
                        .param("date_to", A_DATE_UTC.plusHours(1).toString())
                        .param("process_name", ProcessName.WAVING.name())
        );

        // THEN
        verifyZeroInteractions(getHeadcountEntityUseCase);
        verifyZeroInteractions(getProductivityEntityUseCase);
        verifyZeroInteractions(getThroughputUseCase);
        result.andExpect(status().isOk())
                .andExpect(content()
                .json(getResourceAsString("get_remaining_processing_response.json")));
    }

}
