package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.domain.usecase.forecast.create.CreateForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.create.CreateForecastOutput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.create.CreateForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.remove.DeleteForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.remove.DeleteForecastUseCase;
import com.mercadolibre.planning.model.api.exception.BadRequestException;
import com.mercadolibre.planning.model.api.web.controller.forecast.ForecastController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.stream.Stream;

import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCreateForecastInput;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ForecastController.class)
@SuppressWarnings("PMD.SignatureDeclareThrowsException")
public class ForecastControllerTest {

    private static final String URL = "/planning/model/workflows/{workflow}/forecasts";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CreateForecastUseCase createForecastUseCase;

    @MockBean
    private DeleteForecastUseCase deleteForecastUseCase;

    @DisplayName("Create forecast when path variable is ")
    @ParameterizedTest(name = "{0}")
    @MethodSource("workflowValues")
    public void createForecastOk(final String workflow) throws Exception {
        // GIVEN
        final CreateForecastInput input = mockCreateForecastInput();
        when(createForecastUseCase.execute(input)).thenReturn(new CreateForecastOutput(1));

        // WHEN
        final ResultActions result = mvc.perform(
                post(URL, workflow)
                        .contentType(APPLICATION_JSON)
                        .content(getResourceAsString("post_forecast.json"))
        );

        // THEN
        result.andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1));
    }

    @DisplayName("Delete forecast")
    @Test
    public void deleteOldForecastsOk() throws Exception {
        // GIVEN
        final String url =  URL + "/delete/days/{days}";
        when(deleteForecastUseCase.execute(any(DeleteForecastInput.class)))
                .thenReturn(5);

        // WHEN
        final ResultActions result = mvc.perform(
                post(url, "fbm-wms-outbound", 1)
                        .contentType(APPLICATION_JSON)
        );

        // THEN
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted_rows").value(5));
    }

    @DisplayName("Delete forecast should not accept negative day span")
    @Test
    public void deleteOldForecastsErr() throws Exception {
        // GIVEN
        final String url =  URL + "/delete/days/{days}";
        when(deleteForecastUseCase.execute(any(DeleteForecastInput.class)))
                .thenThrow(BadRequestException.class);

        // WHEN
        final ResultActions result = mvc.perform(
                post(url, "fbm-wms-outbound", -1)
                        .contentType(APPLICATION_JSON)
        );

        // THEN
        result.andExpect(status().isInternalServerError());
    }

    private static Stream<Arguments> workflowValues() {
        return Stream.of(
                Arguments.of("fbm-wms-outbound"),
                Arguments.of("fbm_wms_outbound")
        );
    }
}
