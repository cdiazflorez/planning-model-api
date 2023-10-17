package com.mercadolibre.planning.model.api.web.controller.configuration;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.CONFIG_KEY;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static java.lang.String.format;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.service.sla.OutboundSlaPropertiesService;
import com.mercadolibre.planning.model.api.domain.service.sla.SlaProperties;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.create.ConfigurationInput;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.create.CreateConfigurationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.get.GetConfigurationByKeyUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.get.GetConfigurationInput;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.update.UpdateConfigurationUseCase;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = ConfigurationController.class)
public class ConfigurationControllerTest {

  private static final String LOGISTIC_CENTER_ID = "ARBA01";

  private static final String KEY = "expedition_processing_time";

  private static final String URL = "/planning/model/configuration";

  private static final String UPDATE_URL = "/planning/model/configuration/%s/%s";

  private static final String OLD_CT_PATH = "/logistic_center_id/{lcID}/cycle_time/search";
  private static final String CYCLE_TIME_PATH = "/logistic_center/{lc}/cycle_time/search";

  private static final Instant DATE_FROM = A_DATE_UTC.toInstant();

  private static final Instant DATE_TO = DATE_FROM.plus(3, ChronoUnit.HOURS);

  @Autowired
  private MockMvc mvc;

  @MockBean
  private GetConfigurationByKeyUseCase getConfiguration;

  @MockBean
  private CreateConfigurationUseCase createConfiguration;

  @MockBean
  private UpdateConfigurationUseCase updateConfiguration;

  @MockBean
  private OutboundSlaPropertiesService outboundSlaPropertiesService;

  @Test
  public void testGetConfiguration() throws Exception {
    // GIVEN
    final GetConfigurationInput input = new GetConfigurationInput(LOGISTIC_CENTER_ID, KEY);
    when(getConfiguration.execute(input)).thenReturn(Optional.of(Configuration.builder()
        .logisticCenterId(LOGISTIC_CENTER_ID)
        .key(CONFIG_KEY)
        .value("1")
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
        .value("60")
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
        .value("180")
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

  @DisplayName("Get Outbound Cycle Time Ok")
  @ParameterizedTest
  @ValueSource(strings = {OLD_CT_PATH, CYCLE_TIME_PATH})
  void testGetOutboundCycleTimeOk(
      final String cycleTimePath
  ) throws Exception {
    // GIVEN
    when(outboundSlaPropertiesService.get(
        new OutboundSlaPropertiesService.Input(
            WAREHOUSE_ID,
            FBM_WMS_OUTBOUND,
            DATE_FROM,
            DATE_TO,
            List.of(DATE_FROM, DATE_FROM.plus(1, ChronoUnit.HOURS), DATE_FROM.plus(2, ChronoUnit.HOURS)),
            "UTC"
        )
    )).thenReturn(
        Map.of(
            A_DATE_UTC.toInstant(), new SlaProperties(10L),
            A_DATE_UTC.plusHours(1).toInstant(), new SlaProperties(20L),
            A_DATE_UTC.plusHours(2).toInstant(), new SlaProperties(30L),
            A_DATE_UTC.plusHours(3).toInstant(), new SlaProperties(40L)
        )
    );

    // WHEN
    final ResultActions result = mvc.perform(
        post(URL + cycleTimePath, "ARBA01")
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString("search_cycle_times.json"))
    );

    // THEN
    result.andExpect(status().isOk())
        .andExpect(content().json(getResourceAsString("search_cycle_times_response.json")));
  }
}
