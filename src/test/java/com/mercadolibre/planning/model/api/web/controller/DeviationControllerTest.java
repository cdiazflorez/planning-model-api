package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.SaveForecastDeviationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.SaveForecastDeviationUseCase;
import com.mercadolibre.planning.model.api.web.controller.deviation.DeviationController;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.DeviationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSaveForecastDeviationInput;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DeviationController.class)
public class DeviationControllerTest {

    private static final String URL = "/planning/model/workflows/{workflow}/deviations/save";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SaveForecastDeviationUseCase useCase;

    @DisplayName("Save forecast deviation ")
    @Test
    public void saveDeviationOk() throws Exception {
        // GIVEN
        final SaveForecastDeviationInput input = mockSaveForecastDeviationInput();

        when(useCase.execute(input)).thenReturn(new DeviationResponse(200));

        // WHEN
        final ResultActions result = mvc.perform(
                post(URL, "fbm-wms-outbound")
                        .contentType(APPLICATION_JSON)
                        .content(getResourceAsString("post_forecast_deviation.json"))
        );

        // THEN
        result.andExpect(status().isOk());
    }
}
