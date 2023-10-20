package com.mercadolibre.planning.model.api.web.controller.configuration;

import static com.mercadolibre.planning.model.api.util.TestUtils.LOGISTIC_CENTER_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.ConfigurationUseCase;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = NewConfigurationController.class)
class NewConfigurationControllerTest {

  private static final String URL = "/logistic_center/%s/configuration";

  private static final String KEY_1 = "configuration_01";

  private static final String VALUE_1 = "1.0";

  private static final String KEY_2 = "configuration_02";

  private static final String VALUE_2 = "2.0";

  private static final List<Configuration> CONFIGURATIONS = List.of(
      Configuration.builder()
          .logisticCenterId(LOGISTIC_CENTER_ID)
          .key(KEY_1)
          .value(VALUE_1)
          .build(),
      Configuration.builder()
          .logisticCenterId(LOGISTIC_CENTER_ID)
          .key(KEY_2)
          .value(VALUE_2)
          .build()
  );

  @Autowired
  private MockMvc mvc;

  @MockBean
  private ConfigurationUseCase configurationUseCase;

  @Test
  void testConfigurationSaveOk() throws Exception {
    // GIVEN
    final Map<String, String> configurations = Map.of(
        KEY_1, VALUE_1,
        KEY_2, VALUE_2
    );
    when(configurationUseCase.save(USER_ID, LOGISTIC_CENTER_ID, configurations)).thenReturn(CONFIGURATIONS);
    // WHEN
    final ResultActions result = mvc.perform(
        post(format(URL, LOGISTIC_CENTER_ID))
            .param("user_id", String.valueOf(USER_ID))
            .param("logistic_center_id", LOGISTIC_CENTER_ID)
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("post_new_configuration_ok.json"))
    );
    // THEN
    result.andExpect(status().isOk())
        .andExpect(content().json(getResourceAsString("new_configuration_response.json")));
  }

  @Test
  void testConfigurationError() throws Exception {
    // GIVEN
    // WHEN
    final ResultActions result = mvc.perform(
        post(format(URL, LOGISTIC_CENTER_ID))
            .param("user_id", String.valueOf(USER_ID))
            .param("logistic_center_id", LOGISTIC_CENTER_ID)
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("post_new_configuration_error.json"))
    );
    // THEN
    result.andExpect(status().isBadRequest());
  }

  @ParameterizedTest
  @MethodSource("provideGetConfiguration")
  void testGetConfiguration(final List<Configuration> configurations) throws Exception {
    // GIVEN
    when(configurationUseCase.get(anyString(), anySet())).thenReturn(configurations);
    // WHEN
    final ResultActions result = mvc.perform(
        get(URL)
            .contentType(APPLICATION_JSON)
            .param("logistic_center_id", LOGISTIC_CENTER_ID)
            .param("keys", KEY_1, KEY_2)
    );
    // THEN
    if (configurations.isEmpty()) {
      result.andExpect(status().isNoContent());
    } else {
      result.andExpect(status().isOk())
          .andExpect(content().json(getResourceAsString("new_configuration_response.json")));
    }
  }

  private static Stream<Arguments> provideGetConfiguration() {
    return Stream.of(
        Arguments.of(CONFIGURATIONS),
        Arguments.of(List.of())
    );
  }

}
