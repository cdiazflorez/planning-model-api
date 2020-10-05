package com.mercadolibre.planning.model.api.web.controller.configuration;

import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.GetConfigurationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetConfigurationInput;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Optional;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ConfigurationController.class)
public class ConfigurationControllerTest {

    private static final String LOGISTIC_CENTER_ID = "ARBA01";

    private static final String KEY = "expedition_processing_time";

    private static final String URL = "/planning/model/configuration";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private GetConfigurationUseCase getConfiguration;

    @Test
    public void testGetConfiguration() throws Exception {
        // GIVEN
        final GetConfigurationInput input = new GetConfigurationInput(LOGISTIC_CENTER_ID, KEY);
        when(getConfiguration.execute(input)).thenReturn(Optional.of(new Configuration(
                LOGISTIC_CENTER_ID, KEY, 1, UNITS
        )));

        // WHEN
        final ResultActions result = mvc.perform(get(URL)
                .param("logistic_center_id", LOGISTIC_CENTER_ID)
                .param("key", KEY)
        );

        // THEN
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(1))
                .andExpect(jsonPath("$.metric_unit").value(UNITS.toJson()));
    }

    @Test
    public void testGetConfigurationNotFound() throws Exception {
        // GIVEN
        final GetConfigurationInput input = new GetConfigurationInput(LOGISTIC_CENTER_ID, KEY);
        when(getConfiguration.execute(input)).thenReturn(Optional.empty());

        // WHEN
        final ResultActions result = mvc.perform(get(URL)
                .param("logistic_center_id", LOGISTIC_CENTER_ID)
                .param("key", KEY)
        );

        // THEN
        result.andExpect(status().isNotFound());
    }
}
