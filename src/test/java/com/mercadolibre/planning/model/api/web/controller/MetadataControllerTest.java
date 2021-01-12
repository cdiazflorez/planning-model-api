package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadataInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadataUseCase;
import com.mercadolibre.planning.model.api.web.controller.metadata.MetadataController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
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

    @DisplayName("Get Forecast Metadata")
    @Test
    public void testGetForecastMetadataOk() throws Exception {
        // GIVEN
        final GetForecastMetadataInput input = mockForecastMetadataInput();
        when(getForecastMetadataUseCase.execute(input)).thenReturn(mockForecastByWarehouseId());

        // WHEN
        final ResultActions result = mvc.perform(
                get(URL, "fbm-wms-outbound")
                        .contentType(APPLICATION_JSON)
                        .param("warehouse_id", "ARBA01")
                        .param("date_from", A_DATE_UTC.toString())
                        .param("date_to", A_DATE_UTC.plusDays(2).toString())
        );

        // THEN
        result.andExpect(status().isOk());
    }
}
