package com.mercadolibre.planning.model.api.web.controller;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.domain.usecase.entities.input.EntitySearchFilters.ABILITY_LEVEL;
import static com.mercadolibre.planning.model.api.domain.usecase.entities.input.EntitySearchFilters.PROCESSING_TYPE;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockHeadcountEntityOutput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSearchEntitiesOutput;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.REMAINING_PROCESSING;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.THROUGHPUT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.usecase.entities.SearchEntitiesUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.input.SearchEntitiesInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get.GetMaxCapacityByWarehouseEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get.GetMaxCapacityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.search.SearchEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityController;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

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
  private SearchEntitiesUseCase searchEntitiesUseCase;

  @MockBean
  private SearchEntityUseCase searchEntityUseCase;

  @MockBean
  private GetMaxCapacityEntityUseCase getMaxCapacityEntityUseCase;

  @MockBean
  private GetMaxCapacityByWarehouseEntityUseCase getMaxCapacityByWarehouseEntityUseCase;

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
            .param("processing_type", "effective_workers,active_workers")
            .param("date_from", A_DATE_UTC.toString())
            .param("date_to", A_DATE_UTC.plusDays(2).toString())
    );

    // THEN
    verifyNoInteractions(getProductivityEntityUseCase);
    verifyNoInteractions(getThroughputUseCase);
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
    verifyNoInteractions(getProductivityEntityUseCase);
    result.andExpect(status().isNotFound());
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
    verifyNoInteractions(getProductivityEntityUseCase);
    verifyNoInteractions(getThroughputUseCase);
    verifyNoInteractions(searchEntityUseCase);
    result.andExpect(status().isOk())
        .andExpect(content().json(getResourceAsString("get_headcount_response.json")));
  }

  @DisplayName("Search entities returns all entities")
  @Test
  public void testSearchEntitiesOk() throws Exception {
    // GIVEN
    final var dateFrom = ZonedDateTime.of(2020, 8, 19, 17, 0, 0, 0, ZoneId.of("UTC"));
    final var dateTo = ZonedDateTime.of(2020, 8, 20, 17, 0, 0, 0, ZoneId.of("UTC"));
    when(searchEntitiesUseCase.execute(SearchEntitiesInput.builder()
            .warehouseId(WAREHOUSE_ID)
            .workflow(FBM_WMS_OUTBOUND)
            .dateFrom(dateFrom)
            .dateTo(dateTo)
            .entityTypes(List.of(HEADCOUNT, PRODUCTIVITY, THROUGHPUT, REMAINING_PROCESSING))
            .processName(List.of(PICKING, PACKING))
            .entityFilters(Map.of(
                HEADCOUNT, Map.of(
                    PROCESSING_TYPE.toJson(), List.of(EFFECTIVE_WORKERS.toJson())
                ),
                PRODUCTIVITY, Map.of(ABILITY_LEVEL.toJson(), List.of("1"))
            ))
            .viewDate(dateTo.toInstant())
            .build()
        )
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
