package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.SearchEntitiesUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.input.SearchEntitiesInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.performedprocessing.get.GetPerformedProcessingUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.remainingprocessing.get.GetRemainingProcessingUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.domain.usecase.entities.input.EntitySearchFilters.ABILITY_LEVEL;
import static com.mercadolibre.planning.model.api.domain.usecase.entities.input.EntitySearchFilters.PROCESSING_TYPE;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetPerformedProcessingOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetRemainingProcessingOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockHeadcountEntityOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockProductivityEntityOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSearchEntitiesOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockThroughputEntityOutput;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.REMAINING_PROCESSING;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.THROUGHPUT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @MockBean
    private GetPerformedProcessingUseCase getPerformedProcessingUseCase;

    @MockBean
    private SearchEntitiesUseCase searchEntitiesUseCase;

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
                post(URL, "fbm-wms-outbound", "headcount")
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
                post(URL, "fbm-wms-outbound", "remaining_processing")
                        .contentType(APPLICATION_JSON)
                        .content(getResourceAsString("get_remaining_processing_request.json"))
        );

        // THEN
        verifyZeroInteractions(getHeadcountEntityUseCase);
        verifyZeroInteractions(getProductivityEntityUseCase);
        verifyZeroInteractions(getThroughputUseCase);
        result.andExpect(status().isOk())
                .andExpect(content()
                .json(getResourceAsString("get_remaining_processing_response.json")));
    }

    @DisplayName("Get Performed_processing entity works ok")
    @Test
    public void testGetPerformedProcessingEntityOk() throws Exception {
        // GIVEN
        when(getPerformedProcessingUseCase.execute(any(GetEntityInput.class)))
                .thenReturn(mockGetPerformedProcessingOutput());

        // WHEN
        final ResultActions result = mvc.perform(
                get(URL, "fbm-wms-outbound", "performed_processing")
                        .param("warehouse_id", "ARBA01")
                        .param("date_from", A_DATE_UTC.toString())
                        .param("date_to", A_DATE_UTC.plusHours(1).toString())
                        .param("process_name", "waving")
        );

        // THEN
        verifyZeroInteractions(getHeadcountEntityUseCase);
        verifyZeroInteractions(getProductivityEntityUseCase);
        verifyZeroInteractions(getThroughputUseCase);
        result.andExpect(status().isOk())
                .andExpect(content()
                        .json(getResourceAsString("get_performed_processing_response.json")));
    }

    @DisplayName("Search entities returns all entities")
    @Test
    public void testSearchEntitiesOk() throws Exception {
        // GIVEN
        when(searchEntitiesUseCase.execute(SearchEntitiesInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .dateFrom(ZonedDateTime.of(2020, 8, 19, 17, 0, 0, 0, ZoneId.of("UTC")))
                .dateTo(ZonedDateTime.of(2020, 8, 20, 17, 0, 0, 0, ZoneId.of("UTC")))
                .entityTypes(List.of(HEADCOUNT, PRODUCTIVITY, THROUGHPUT, REMAINING_PROCESSING))
                .processName(List.of(PICKING, PACKING))
                .entityFilters(Map.of(
                        HEADCOUNT, Map.of(
                                PROCESSING_TYPE.toJson(), List.of(ACTIVE_WORKERS.toJson())
                        ),
                        PRODUCTIVITY, Map.of(ABILITY_LEVEL.toJson(), List.of("1"))
                ))
                .build())
        ).thenReturn(mockSearchEntitiesOutput());

        // WHEN
        final ResultActions result = mvc.perform(
                post(URL, "fbm-wms-outbound", "search")
                        .contentType(APPLICATION_JSON)
                        .content(getResourceAsString("search_entities_request.json"))
        );

        // THEN
        result.andExpect(status().isOk())
                .andExpect(content()
                        .json(getResourceAsString("search_entities_response.json")));
    }

}
