package com.mercadolibre.planning.model.api.web.controller.deferral;

import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.GetDeferralReport;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.SaveOutboundDeferralReport;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

@WebMvcTest(DeferralController.class)
@Import(Msg.class)
class DeferralControllerTest {

  private static final String URL_FILE = "controller/deferral/deferral_request.json";

  private static final String URL_FILE_BAD_REQUEST = "controller/deferral/deferral_bad_request.json";

  private static final String URL_FILE_BAD_REQUEST_NULL = "controller/deferral/deferral_bad_request_null.json";

  private static final String URL_FILE_BAD_REQUEST_NOT_OBJECT = "controller/deferral/deferral_bad_request_not_object.json";

  private static final String URL_FILE_BAD_REQUEST_DEFERRAL_AFTER_NOW = "controller/deferral/deferral_bad_request_after_now.json";

  private static final String URL = "/planning/model/deferred/save";

  private static final String DEFERRAL_URL = "/planning/model/deferred/event";

  private static final String URL_GET = "/planning/model/deferred";

  private static final String URL_FILE_GET = "controller/deferral/deferral_report.json";

  private static final String LOGISTIC_CENTER_ID = "ARBA01";

  private static final Instant NOW = Instant.parse("2023-07-20T16:00:00Z");

  private static final Instant AFTER_NOW = Instant.parse("2023-07-20T19:00:00Z");

  private static final Instant CPT = Instant.parse("2023-07-21T16:00:00Z");

  private static final Instant CPT1 = Instant.parse("2023-07-21T17:00:00Z");

  private static final Instant CPT2 = Instant.parse("2023-07-21T18:00:00Z");

  private static final Instant DATE_FROM = Instant.parse("2023-07-21T16:00:00Z");

  private static final Instant DATE_TO = Instant.parse("2023-07-22T16:00:00Z");

  private static final String EMPTY_JSON = "{}";

  private static final List<SaveOutboundDeferralReport.CptDeferralReport> CPT_DEFERRAL_REPORTS = List.of(
      new SaveOutboundDeferralReport.CptDeferralReport(CPT, true, DeferralType.CAP_MAX),
      new SaveOutboundDeferralReport.CptDeferralReport(CPT1, true, DeferralType.CASCADE),
      new SaveOutboundDeferralReport.CptDeferralReport(CPT2, true, DeferralType.NOT_DEFERRED)
  );

  private static final DeferralResponse DEFERRAL_RESPONSE_SUCCESS = new DeferralResponse(
      HttpStatus.CREATED.value(), HttpStatus.CREATED.toString());

  private static final DeferralResponse DEFERRAL_RESPONSE_BAD_REQUEST = new DeferralResponse(
      HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.toString());

  private static final DeferralResponse DEFERRAL_RESPONSE_INTERNAL_ERROR = new DeferralResponse(
      HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.toString());

  private static final Map<Instant, List<GetDeferralReport.SlaStatus>> DEFERRED = Map.of(
      DATE_FROM.plus(1, ChronoUnit.HOURS),
      List.of(
          new GetDeferralReport.SlaStatus(CPT, DeferralType.CASCADE),
          new GetDeferralReport.SlaStatus(CPT1, DeferralType.CASCADE),
          new GetDeferralReport.SlaStatus(CPT2, DeferralType.CAP_MAX)
      ),
      DATE_FROM.plus(2, ChronoUnit.HOURS),
      List.of(
          new GetDeferralReport.SlaStatus(CPT, DeferralType.NOT_DEFERRED),
          new GetDeferralReport.SlaStatus(CPT1, DeferralType.NOT_DEFERRED),
          new GetDeferralReport.SlaStatus(CPT2, DeferralType.NOT_DEFERRED)
      )
  );

  @Autowired
  private MockMvc mvc;

  @MockBean
  private SaveOutboundDeferralReport saveOutboundDeferralReport;

  @MockBean
  private GetDeferralReport getDeferralReport;

  private static Stream<Arguments> parametersSaveStatus() {
    return Stream.of(
        Arguments.of(
            URL_FILE,
            status().isCreated(),
            1,
            NOW,
            DEFERRAL_RESPONSE_SUCCESS
        ),
        Arguments.of(
            URL_FILE_BAD_REQUEST,
            status().isBadRequest(),
            0,
            NOW,
            DEFERRAL_RESPONSE_BAD_REQUEST
        ),
        Arguments.of(
            URL_FILE_BAD_REQUEST_NULL,
            status().isBadRequest(),
            0,
            NOW,
            DEFERRAL_RESPONSE_BAD_REQUEST
        ),
        Arguments.of(
            URL_FILE_BAD_REQUEST_NOT_OBJECT,
            status().isBadRequest(),
            0,
            NOW,
            DEFERRAL_RESPONSE_BAD_REQUEST
        ),
        Arguments.of(
            URL_FILE_BAD_REQUEST_DEFERRAL_AFTER_NOW,
            status().isBadRequest(),
            1,
            AFTER_NOW,
            DEFERRAL_RESPONSE_BAD_REQUEST
        ),
        Arguments.of(
            URL_FILE,
            status().isInternalServerError(),
            1,
            NOW,
            DEFERRAL_RESPONSE_INTERNAL_ERROR
        )
    );
  }

  private static Stream<Arguments> parameterGetTest() throws IOException {
    return Stream.of(
        Arguments.of(DEFERRED, LOGISTIC_CENTER_ID, DATE_FROM.toString(), DATE_TO.toString(), status().isOk(),
            getResourceAsString(URL_FILE_GET)),
        Arguments.of(null, null, null, null, status().isBadRequest(), EMPTY_JSON),
        Arguments.of(null, null, DATE_FROM.toString(), DATE_TO.toString(), status().isBadRequest(), EMPTY_JSON),
        Arguments.of(null, LOGISTIC_CENTER_ID, null, null, status().isBadRequest(), EMPTY_JSON),
        Arguments.of(null, null, DATE_FROM.toString(), null, status().isBadRequest(), EMPTY_JSON),
        Arguments.of(null, null, null, DATE_TO.toString(), status().isBadRequest(), EMPTY_JSON),
        Arguments.of(null, LOGISTIC_CENTER_ID, DATE_TO.toString(), DATE_FROM.toString(), status().isBadRequest(), EMPTY_JSON)
    );
  }

  @ParameterizedTest
  @MethodSource("parameterGetTest")
  void testGet(final Map<Instant, List<GetDeferralReport.SlaStatus>> deferred, final String logisticCenterId, final String dateFrom,
               final String dateTo, final ResultMatcher status, final String expected) throws Exception {
    //GIVEN
    when(getDeferralReport.execute(LOGISTIC_CENTER_ID, DATE_FROM, DATE_TO))
        .thenReturn(deferred);

    // WHEN
    final ResultActions result = mvc.perform(get(URL_GET)
        .param("logistic_center_id", logisticCenterId)
        .param("date_from", dateFrom)
        .param("date_to", dateTo)
    );

    result.andExpect(status)
        .andExpect(content().json(expected));
  }

  @ParameterizedTest
  @MethodSource("parametersSaveStatus")
  void testSaveDeferralEvent(
      final String url,
      final ResultMatcher statusController,
      final int times,
      final Instant deferralDate,
      final DeferralResponse deferralResponse
  ) throws Exception {
    // GIVEN
    when(saveOutboundDeferralReport.save(LOGISTIC_CENTER_ID, deferralDate, CPT_DEFERRAL_REPORTS))
        .thenReturn(deferralResponse);

    // WHEN
    final ResultActions result = mvc.perform(
        post(DEFERRAL_URL)
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString(url))
    );

    // THEN
    result.andExpect(statusController);
    verify(saveOutboundDeferralReport, times(times)).save(LOGISTIC_CENTER_ID, deferralDate, CPT_DEFERRAL_REPORTS);
  }
}
