package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.domain.usecase.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.strategy.GetEntityStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetHeadcountEntityOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetProductivityEntityOutput;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.PRODUCTIVITY;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EntityController.class)
public class EntityControllerTest {

    private static final String URL = "/planning/model/workflows/{workflow}/entities/{entity}";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private GetEntityStrategy getEntityStrategy;

    @MockBean
    private GetHeadcountEntityUseCase getHeadcountEntityUseCase;

    @MockBean
    private GetProductivityEntityUseCase getProductivityEntityUseCase;

    @DisplayName("Get headcount entity works ok")
    @Test
    public void testGetHeadcountEntityOk() throws Exception {
        // GIVEN
        when(getHeadcountEntityUseCase.supportsEntityType(HEADCOUNT))
                .thenReturn(true);

        when(getEntityStrategy.getBy(HEADCOUNT))
                .thenReturn(getHeadcountEntityUseCase);

        when(getHeadcountEntityUseCase.execute(any(GetEntityInput.class)))
                .thenReturn(mockGetHeadcountEntityOutput());

        // WHEN
        final ResultActions result = mvc.perform(
                get(URL, "fbm-wms-outbound", "headcount")
                        .contentType(APPLICATION_JSON)
                        .param("warehouse_id", "ARBA01")
                        .param("source", "forecast")
                        .param("process_name", "picking,packing")
                        .param("date_from", A_DATE_UTC.toString())
                        .param("date_to", A_DATE_UTC.plusDays(2).toString())
        );

        // THEN
        verifyZeroInteractions(getProductivityEntityUseCase);
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
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.error", is("invalid_entity_type")))
                .andExpect(jsonPath("$.message",
                        containsString("Value unknown is invalid, instead it should be"
                                + " one of [HEADCOUNT, PRODUCTIVITY, THROUGHPUT]")));
    }

    @DisplayName("Get headcount entity when entity is not part of the enum")
    @Test
    public void testGetHeadcountEntityWithInvalidEntityType() throws Exception {
        // WHEN
        final ResultActions result = mvc.perform(
                get(URL, "fbm-wms-outbound", "invalid")
                        .contentType(APPLICATION_JSON)
                        .param("warehouse_id", "ARBA01")
                        .param("source", "forecast")
                        .param("process_name", "picking,packing")
                        .param("date_from", A_DATE_UTC.toString())
                        .param("date_to", A_DATE_UTC.plusDays(2).toString())
        );

        // THEN
        result.andExpect(status().isBadRequest());
        verifyZeroInteractions(getHeadcountEntityUseCase);
        verifyZeroInteractions(getProductivityEntityUseCase);
    }

    @DisplayName("Get productivity entity works ok")
    @Test
    public void testGetProductivityEntityOk() throws Exception {
        // GIVEN
        when(getHeadcountEntityUseCase.supportsEntityType(PRODUCTIVITY))
                .thenReturn(false);

        when(getProductivityEntityUseCase.supportsEntityType(PRODUCTIVITY))
                .thenReturn(true);

        when(getEntityStrategy.getBy(PRODUCTIVITY))
                .thenReturn(getProductivityEntityUseCase);

        when(getProductivityEntityUseCase.execute(any(GetEntityInput.class)))
                .thenReturn(mockGetProductivityEntityOutput());

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
        result.andExpect(status().isOk())
                .andExpect(content().json(getResourceAsString("get_productivity_response.json")));
    }
}
