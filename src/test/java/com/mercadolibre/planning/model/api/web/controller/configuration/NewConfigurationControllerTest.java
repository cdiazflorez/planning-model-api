package com.mercadolibre.planning.model.api.web.controller.configuration;

import static com.mercadolibre.planning.model.api.util.TestUtils.LOGISTIC_CENTER_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static java.lang.String.format;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.ConfigurationUseCase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = NewConfigurationController.class)
public class NewConfigurationControllerTest {

  private static final String URL = "/logistic_center/%s/configuration";

  private static final String KEY_1 = "configuration_01";

  private static final String VALUE_1 = "1.0";

  private static final String KEY_2 = "configuration_02";

  private static final String VALUE_2 = "2.0";

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
    when(configurationUseCase.save(USER_ID, LOGISTIC_CENTER_ID, configurations)).thenReturn(
        List.of(
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
        )
    );
    // WHEN
    final ResultActions result = mvc.perform(
        post(format(URL, LOGISTIC_CENTER_ID))
            .param("user_id", String.valueOf(USER_ID))
            .param("logistic_center_id", LOGISTIC_CENTER_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getResourceAsString("post_new_configuration_ok.json"))
    );
    // THEN
    result.andExpect(status().isOk())
        .andExpect(content().json(getResourceAsString("post_new_configuration_ok.json")));
  }

  @Test
  void testConfigurationError() throws Exception {
    // GIVEN
    // WHEN
    final ResultActions result = mvc.perform(
        post(format(URL, LOGISTIC_CENTER_ID))
            .param("user_id", String.valueOf(USER_ID))
            .param("logistic_center_id", LOGISTIC_CENTER_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getResourceAsString("post_new_configuration_error.json"))
    );
    // THEN
    result.andExpect(status().isBadRequest());
  }

}
