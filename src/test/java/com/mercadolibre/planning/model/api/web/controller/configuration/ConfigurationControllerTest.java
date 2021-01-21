package com.mercadolibre.planning.model.api.web.controller.configuration;

import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.CreateConfigurationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.GetConfigurationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.UpdateConfigurationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.input.ConfigurationInput;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetConfigurationInput;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Optional;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.util.TestUtils.CONFIG_KEY;
import static com.mercadolibre.planning.model.api.util.TestUtils.LOGISTIC_CENTER_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static java.lang.String.format;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ConfigurationController.class)
public class ConfigurationControllerTest {

    private static final String LOGISTIC_CENTER_ID = "ARBA01";

    private static final String KEY = "expedition_processing_time";

    private static final String URL = "/planning/model/configuration";

    private static final String UPDATE_URL = "/planning/model/configuration/%s/%s";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private GetConfigurationUseCase getConfiguration;

    @MockBean
    private CreateConfigurationUseCase createConfiguration;

    @MockBean
    private UpdateConfigurationUseCase updateConfiguration;

    @Test
    public void testGetConfiguration() throws Exception {
        // GIVEN
        final GetConfigurationInput input = new GetConfigurationInput(LOGISTIC_CENTER_ID, KEY);
        when(getConfiguration.execute(input)).thenReturn(Optional.of(Configuration.builder()
                .logisticCenterId(LOGISTIC_CENTER_ID)
                .key(CONFIG_KEY)
                .value(1)
                .metricUnit(UNITS)
                .build()));

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

    @Test
    public void testCreateConfiguration() throws Exception {
        // GIVEN
        final ConfigurationInput input = new ConfigurationInput(
                LOGISTIC_CENTER_ID, KEY, 60, MINUTES);

        when(createConfiguration.execute(input)).thenReturn(Configuration.builder()
                .logisticCenterId(LOGISTIC_CENTER_ID)
                .key(CONFIG_KEY)
                .value(60)
                .metricUnit(MINUTES)
                .build());

        // WHEN
        final ResultActions result = mvc.perform(
                post(URL)
                        .contentType(APPLICATION_JSON)
                        .content(getResourceAsString("post_configuration.json"))
        );

        // THEN
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(60))
                .andExpect(jsonPath("$.metric_unit").value(MINUTES.toJson()));
    }

    @Test
    public void testUpdateConfiguration() throws Exception {
        // GIVEN
        final ConfigurationInput input = new ConfigurationInput(
                LOGISTIC_CENTER_ID, KEY, 180, MINUTES);

        when(updateConfiguration.execute(input)).thenReturn(Configuration.builder()
                .logisticCenterId(LOGISTIC_CENTER_ID)
                .key(CONFIG_KEY)
                .value(180)
                .metricUnit(MINUTES)
                .build());

        // WHEN
        final ResultActions result = mvc.perform(
                put(format(UPDATE_URL, LOGISTIC_CENTER_ID, KEY))
                        .contentType(APPLICATION_JSON)
                        .content(getResourceAsString("put_configuration.json"))
        );

        // THEN
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(180))
                .andExpect(jsonPath("$.metric_unit").value(MINUTES.toJson()));
    }
}
