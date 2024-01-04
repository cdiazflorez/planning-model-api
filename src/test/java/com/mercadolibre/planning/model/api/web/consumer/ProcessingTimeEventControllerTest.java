package com.mercadolibre.planning.model.api.web.consumer;

import static com.mercadolibre.planning.model.api.web.consumer.ConsumerMessageDTO.ProcessingTimeToUpdate;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.mercadolibre.planning.model.api.domain.service.configuration.DayAndHourProcessingTime;
import com.mercadolibre.planning.model.api.domain.service.configuration.ProcessingTimeService;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = ProcessingTimeEventController.class)
public class ProcessingTimeEventControllerTest {

  private static final String LOGISTIC_CENTER_ID = "ARBA01";

  private static final String ID = "ARBA01_SCN1_B_2736";

  private static final String EVENT_TYPE_NO_VALID = "UpdateSpecialDate";

  private static final String CREATE_ETS = "CreateETS";

  private static final String DELETE_ETS = "DeleteETS";

  private static final String UPDATE_ETS = "UpdateETS";

  private static final String WEDNESDAY = "wednesday";

  private static final String MONDAY = "monday";

  private static final String TUESDAY = "tuesday";

  private static final String CPT_13 = "1300";

  private static final String CPT_21 = "2100";

  private static final String CPT_18 = "1830";

  private static final String CPT_10 = "1000";

  private static final int PROCESSING_TIME_240 = 240;

  private static final Long DATE = 1830L;

  private static final ProcessingTimeToUpdate NO_VALID_EVENT_TYPE = new ProcessingTimeToUpdate(
      ID,
      LOGISTIC_CENTER_ID,
      EVENT_TYPE_NO_VALID
  );
  private static final ConsumerMessageDTO CUSTOMER_RESPONSE_INVALID_EVENT = new ConsumerMessageDTO(
      ID,
      NO_VALID_EVENT_TYPE,
      DATE
  );
  private static final ProcessingTimeToUpdate CREATE_ETS_DTO = new ProcessingTimeToUpdate(
      ID,
      LOGISTIC_CENTER_ID,
      CREATE_ETS
  );
  private static final ConsumerMessageDTO CUSTOMER_RESPONSE_CREATE = new ConsumerMessageDTO(
      ID,
      CREATE_ETS_DTO,
      DATE
  );
  private static final ProcessingTimeToUpdate DELETE_ETS_DTO = new ProcessingTimeToUpdate(
      ID,
      LOGISTIC_CENTER_ID,
      DELETE_ETS
  );
  private static final ConsumerMessageDTO CUSTOMER_RESPONSE_DELETE = new ConsumerMessageDTO(
      ID,
      DELETE_ETS_DTO,
      DATE
  );
  private static final ProcessingTimeToUpdate UPDATE_ETS_DTO = new ProcessingTimeToUpdate(
      ID,
      LOGISTIC_CENTER_ID,
      UPDATE_ETS
  );
  private static final ConsumerMessageDTO CUSTOMER_RESPONSE_UPDATE = new ConsumerMessageDTO(
      ID,
      UPDATE_ETS_DTO,
      DATE
  );

  private static final List<DayAndHourProcessingTime> MOCK_EMPTY = List.of();

  private static final List<DayAndHourProcessingTime> MOCK_OK = List.of(
      new DayAndHourProcessingTime(LOGISTIC_CENTER_ID, MONDAY, CPT_18, PROCESSING_TIME_240),
      new DayAndHourProcessingTime(LOGISTIC_CENTER_ID, MONDAY, CPT_10, PROCESSING_TIME_240),
      new DayAndHourProcessingTime(LOGISTIC_CENTER_ID, WEDNESDAY, CPT_21, PROCESSING_TIME_240),
      new DayAndHourProcessingTime(LOGISTIC_CENTER_ID, TUESDAY, CPT_13, PROCESSING_TIME_240),
      new DayAndHourProcessingTime(LOGISTIC_CENTER_ID, TUESDAY, CPT_21, PROCESSING_TIME_240)
  );
  private static final ProcessingTimeConsumerResponse EXPECTED_OK = new ProcessingTimeConsumerResponse(
      HttpStatus.OK.value(),
      String.format("[processing_time_event] Update of warehouse ARBA01 successfully performed, new data are %s", getMockOk())
  );
  private static final ProcessingTimeConsumerResponse EXPECTED_NO_VALID_EVENT_TYPE = new ProcessingTimeConsumerResponse(
      HttpStatus.NO_CONTENT.value(),
      "[processing_time_event] Event UpdateSpecialDate is not valid to run the process, "
      + "only [CREATE_ETS, DELETE_ETS, UPDATE_ETS] is allowed."
  );
  private static final ProcessingTimeConsumerResponse EXPECTED_IS_EMPTY = new ProcessingTimeConsumerResponse(
      HttpStatus.NO_CONTENT.value(),
      "[processing_time_event] no value returned from the use case for warehouse ARBA01."
  );

  @Autowired
  private MockMvc mvc;

  @MockBean
  private ProcessingTimeService processingTimeService;

  private static String fromDtoToString(final ConsumerMessageDTO consumerResponse) throws JsonProcessingException {
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    return ow.writeValueAsString(consumerResponse);
  }

  private static String fromDtoToString(final ProcessingTimeConsumerResponse consumerResponse) throws JsonProcessingException {
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    return ow.writeValueAsString(consumerResponse);
  }

  private static List<DayAndHourProcessingTime> getMockOk() {
    return MOCK_OK;
  }

  public static Stream<Arguments> testArguments() throws JsonProcessingException {
    return Stream.of(
        Arguments.of(MOCK_EMPTY,
            fromDtoToString(CUSTOMER_RESPONSE_INVALID_EVENT),
            HttpStatus.NO_CONTENT,
            fromDtoToString(EXPECTED_NO_VALID_EVENT_TYPE)),
        Arguments.of(MOCK_EMPTY,
            fromDtoToString(CUSTOMER_RESPONSE_CREATE),
            HttpStatus.NO_CONTENT,
            fromDtoToString(EXPECTED_IS_EMPTY)),
        Arguments.of(MOCK_OK,
            fromDtoToString(CUSTOMER_RESPONSE_DELETE),
            HttpStatus.OK,
            fromDtoToString(EXPECTED_OK)),
        Arguments.of(MOCK_OK,
            fromDtoToString(CUSTOMER_RESPONSE_UPDATE),
            HttpStatus.OK,
            fromDtoToString(EXPECTED_OK))
    );
  }

  @ParameterizedTest
  @MethodSource("testArguments")
  public void testProcessingTimeConsumer(
      final List<DayAndHourProcessingTime> mock,
      final String request,
      final HttpStatus httpStatus,
      final String expected
  ) throws Exception {
    lenient().when(processingTimeService.updateProcessingTimeForCptsByLogisticCenter(LOGISTIC_CENTER_ID))
        .thenReturn(mock);

    final String url = "/consumer/processing_time/event";

    final ResultActions resultActions = mvc.perform(
        post(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(request)
    );

    resultActions.andExpect(status().is(httpStatus.value()))
        .andExpect(content().json(expected));
  }
}
