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
import com.mercadolibre.planning.model.api.domain.service.configuration.DayAndHourProcessingTime;
import com.mercadolibre.planning.model.api.domain.service.configuration.ProcessingTimeService;
import com.mercadolibre.planning.model.api.domain.service.configuration.SlaProcessingTimes;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.ConfigurationUseCase;
import com.mercadolibre.planning.model.api.exception.ProcessingTimeException;
import com.mercadolibre.planning.model.api.exception.ReadFuryConfigException;
import java.time.Instant;
import java.time.ZoneId;
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

  private static final String PROCESSING_TIME_PATH = "/processing_time";

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

  private static final String DATE_FROM_KEY = "date_from";

  private static final String DATE_TO_KEY = "date_to";

  private static final String ZONE_ID_KEY = "zone_id";

  private static final String JSON_MORE_THAN_WEEK = "controller/configuration/invalid_date_range_more_than_week.json";

  private static final String JSON_FROM_AFTER_TO = "controller/configuration/invalid_date_range_from_after_to.json";

  private static final String JSON_PROCESSING_TIME_EXCEPTION = "controller/configuration/processing_time_exception.json";

  private static final String JSON_UPDATE_PROCESSING_TIMES = "controller/configuration/update_processing_times.json";

  private static final Object JSON_FURY_CONFIG_EXCEPTION = "controller/configuration/fury_config_exception.json";

  private static final int BAD_REQUEST = 400;

  private static final int CONFLICT = 409;

  private static final String WEDNESDAY = "wednesday";

  private static final String MONDAY = "monday";

  private static final String TUESDAY = "tuesday";

  private static final String CPT_13 = "1300";

  private static final String CPT_21 = "2100";

  private static final String CPT_18 = "1830";

  private static final String CPT_10 = "1000";

  private static final String ZONE_ID = "America/Buenos_Aires";

  private static final String PT_DATE_FROM = "2023-12-11T15:00:00Z";

  private static final String PT_DATE_TO_PLUS_DAYS = "2023-12-14T23:59:00Z";

  private static final String PT_DATE_TO_PLUS_WEEK = "2023-12-20T23:59:00Z";

  private static final Instant SLA_DATE_11_1600 = Instant.parse("2023-12-11T16:00:00Z");

  private static final Instant SLA_DATE_11_2130 = Instant.parse("2023-12-11T21:30:00Z");

  private static final Instant SLA_DATE_12_1300 = Instant.parse("2023-12-12T13:00:00Z");

  private static final Instant SLA_DATE_14_0000 = Instant.parse("2023-12-14T00:00:00Z");

  private static final int PROCESSING_TIME_240 = 240;

  private static final int PROCESSING_TIME_165 = 165;

  private static final List<SlaProcessingTimes.SlaProperties> RESPONSE_PROPERTIES_3_DAYS = List.of(
      new SlaProcessingTimes.SlaProperties(
          SLA_DATE_11_1600,
          PROCESSING_TIME_240
      ),
      new SlaProcessingTimes.SlaProperties(
          SLA_DATE_11_2130,
          PROCESSING_TIME_240
      ),
      new SlaProcessingTimes.SlaProperties(
          SLA_DATE_12_1300,
          PROCESSING_TIME_240
      ),
      new SlaProcessingTimes.SlaProperties(
          SLA_DATE_14_0000,
          PROCESSING_TIME_240
      )
  );

  private static final SlaProcessingTimes SLA_PROCESSING_TIMES_165 = new SlaProcessingTimes(
      PROCESSING_TIME_165,
      RESPONSE_PROPERTIES_3_DAYS
  );

  private static final List<DayAndHourProcessingTime> OUTBOUND_PROCESSING_TIMES_3_DAYS = List.of(
      new DayAndHourProcessingTime(LOGISTIC_CENTER_ID, WEDNESDAY, CPT_21, PROCESSING_TIME_240),
      new DayAndHourProcessingTime(LOGISTIC_CENTER_ID, MONDAY, CPT_18, PROCESSING_TIME_240),
      new DayAndHourProcessingTime(LOGISTIC_CENTER_ID, MONDAY, CPT_13, PROCESSING_TIME_240),
      new DayAndHourProcessingTime(LOGISTIC_CENTER_ID, TUESDAY, CPT_10, PROCESSING_TIME_240)
  );

  private static final ProcessingTimeException PROCESSING_TIME_EXCEPTION = new ProcessingTimeException(
      "Processing Time Error",
      new RuntimeException()
  );

  private static final ReadFuryConfigException FURY_CONFIG_EXCEPTION = new ReadFuryConfigException(
      "Fury Config Error",
      new RuntimeException()
  );

  @Autowired
  private MockMvc mvc;

  @MockBean
  private ConfigurationUseCase configurationUseCase;

  @MockBean
  private ProcessingTimeService processingTimeService;

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

  @Test
  void testProcessingTimeForCptInDateRange() throws Exception {
    // GIVEN
    when(processingTimeService.getProcessingTimeForCptInDateRange(
        LOGISTIC_CENTER_ID,
        Instant.parse(PT_DATE_FROM),
        Instant.parse(PT_DATE_TO_PLUS_DAYS),
        ZoneId.of(ZONE_ID)
    )).thenReturn(SLA_PROCESSING_TIMES_165);

    // WHEN
    final ResultActions result = mvc.perform(get(format(URL + PROCESSING_TIME_PATH, LOGISTIC_CENTER_ID))
        .param(DATE_FROM_KEY, PT_DATE_FROM)
        .param(DATE_TO_KEY, PT_DATE_TO_PLUS_DAYS)
        .param(ZONE_ID_KEY, ZONE_ID)
    );

    // THEN
    result.andExpect(status().isOk())
        .andExpect(content().json(getResourceAsString("controller/configuration/default_processing_times.json")));
  }

  @ParameterizedTest
  @MethodSource("testProcessingTimeForCptInDateRangeExceptionsProvider")
  void testProcessingTimeForCptInDateRangeExceptions(
      final String dateFrom,
      final String dateTo,
      final String jsonFilePath,
      final int httpStatus,
      final Throwable serviceException
  ) throws Exception {
    // GIVEN
    when(processingTimeService.getProcessingTimeForCptInDateRange(
        LOGISTIC_CENTER_ID,
        Instant.parse(PT_DATE_FROM),
        Instant.parse(PT_DATE_TO_PLUS_DAYS),
        ZoneId.of(ZONE_ID)
    )).thenThrow(serviceException);

    // WHEN
    final ResultActions result = mvc.perform(get(format(URL + PROCESSING_TIME_PATH, LOGISTIC_CENTER_ID))
        .param(DATE_FROM_KEY, dateFrom)
        .param(DATE_TO_KEY, dateTo)
        .param(ZONE_ID_KEY, ZONE_ID)
    );

    // THEN
    result.andExpect(status().is(httpStatus))
        .andExpect(content().json(getResourceAsString(jsonFilePath)));
  }

  @Test
  void testUpdateProcessingTimeForCptsByLogisticCenter() throws Exception {
    // GIVEN
    when(processingTimeService.updateProcessingTimeForCptsByLogisticCenter(
        LOGISTIC_CENTER_ID
    )).thenReturn(OUTBOUND_PROCESSING_TIMES_3_DAYS);

    // WHEN
    final ResultActions result = mvc.perform(post(format(URL + PROCESSING_TIME_PATH, LOGISTIC_CENTER_ID))
        .param(DATE_FROM_KEY, PT_DATE_FROM)
        .param(DATE_TO_KEY, PT_DATE_TO_PLUS_DAYS)
        .param(ZONE_ID_KEY, ZONE_ID)
    );

    // THEN
    result.andExpect(status().isOk())
        .andExpect(content().json(getResourceAsString(JSON_UPDATE_PROCESSING_TIMES)));
  }

  @Test
  void testUpdateProcessingTimeForCptsByLogisticCenterException() throws Exception {
    // GIVEN
    when(processingTimeService.updateProcessingTimeForCptsByLogisticCenter(LOGISTIC_CENTER_ID))
        .thenThrow(PROCESSING_TIME_EXCEPTION);

    // WHEN
    final ResultActions result = mvc.perform(post(format(URL + PROCESSING_TIME_PATH, LOGISTIC_CENTER_ID))
        .param(DATE_FROM_KEY, PT_DATE_FROM)
        .param(DATE_TO_KEY, PT_DATE_TO_PLUS_DAYS)
        .param(ZONE_ID_KEY, ZONE_ID)
    );

    //THEN
    result.andExpect(status().is(CONFLICT))
        .andExpect(content().json(getResourceAsString(JSON_PROCESSING_TIME_EXCEPTION)));
  }

  private static Stream<Arguments> provideGetConfiguration() {
    return Stream.of(
        Arguments.of(CONFIGURATIONS),
        Arguments.of(List.of())
    );
  }

  public static Stream<Arguments> testProcessingTimeForCptInDateRangeExceptionsProvider() {
    return Stream.of(
        Arguments.of(PT_DATE_TO_PLUS_DAYS, PT_DATE_FROM, JSON_FROM_AFTER_TO, BAD_REQUEST, PROCESSING_TIME_EXCEPTION),
        Arguments.of(PT_DATE_FROM, PT_DATE_TO_PLUS_WEEK, JSON_MORE_THAN_WEEK, BAD_REQUEST, PROCESSING_TIME_EXCEPTION),
        Arguments.of(PT_DATE_FROM, PT_DATE_TO_PLUS_DAYS, JSON_PROCESSING_TIME_EXCEPTION, CONFLICT, PROCESSING_TIME_EXCEPTION),
        Arguments.of(PT_DATE_FROM, PT_DATE_TO_PLUS_DAYS, JSON_FURY_CONFIG_EXCEPTION, CONFLICT, FURY_CONFIG_EXCEPTION)
    );
  }

}
