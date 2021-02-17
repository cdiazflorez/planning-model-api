package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadataInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadataUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.web.controller.metadata.GetForecastMetadataRequest;
import com.mercadolibre.planning.model.api.web.controller.metadata.MetadataController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_IN;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_OUT;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockForecastByWarehouseId;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockForecastMetadataInput;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MetadataController.class)
public class MetadataControllerTest {

    private static final String URL = "/planning/model/workflows/{workflow}/metadata";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private GetForecastMetadataUseCase getForecastMetadataUseCase;

    @MockBean
    private GetForecastUseCase getForecastUseCase;

    @DisplayName("Get Forecast Metadata")
    @Test
    public void testGetForecastMetadataOk() throws Exception {
        // GIVEN
        final GetForecastMetadataRequest request = new GetForecastMetadataRequest(
                WAREHOUSE_ID,
                DATE_IN,
                DATE_OUT
        );
        final GetForecastMetadataInput input = mockForecastMetadataInput();

        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(request.getWarehouseId())
                .dateFrom(request.getDateFrom())
                .dateTo(request.getDateTo())
                .build())
        ).thenReturn(input.getForecastIds());

        when(getForecastMetadataUseCase.execute(input)).thenReturn(mockForecastByWarehouseId());

        // WHEN
        final ResultActions result = mvc.perform(
                get(URL, "fbm-wms-outbound")
                        .contentType(APPLICATION_JSON)
                        .param("warehouse_id", WAREHOUSE_ID)
                        .param("date_from", request.getDateFrom().toString())
                        .param("date_to", request.getDateTo().toString())
        );

        // THEN
        result.andExpect(status().isOk());
    }
}
