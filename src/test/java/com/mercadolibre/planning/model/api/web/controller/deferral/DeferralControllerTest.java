package com.mercadolibre.planning.model.api.web.controller.deferral;

import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.SaveDeferralReport;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

@WebMvcTest(DeferralController.class)
@Import(Msg.class)
public class DeferralControllerTest {

  private static final String URL_FILE = "controller/deferral/deferral_request.json";

  private static final String URL_FILE_BAD_REQUEST = "controller/deferral/deferral_bad_request.json";

  private static final String URL_FILE_BAD_REQUEST_NULL = "controller/deferral/deferral_bad_request_null.json";

  private static final String URL_FILE_BAD_REQUEST_NOT_OBJECT = "controller/deferral/deferral_bad_request_not_object.json";

  private static final String URL = "/planning/model/deferred/save";

  private static final String LOGISTIC_CENTER_ID = "ARBA01";

  private static final Instant NOW = Instant.parse("2023-07-20T16:00:00Z");

  private static final Instant CPT = Instant.parse("2023-07-21T16:00:00Z");

  private static final Instant CPT1 = Instant.parse("2023-07-21T17:00:00Z");

  private static final Instant CPT2 = Instant.parse("2023-07-21T18:00:00Z");

  private static final List<SaveDeferralReport.SlaDeferredReport> SLA_DEFERRED = List.of(
      new SaveDeferralReport.SlaDeferredReport(CPT, DeferralType.CAP_MAX),
      new SaveDeferralReport.SlaDeferredReport(CPT1, DeferralType.CASCADE),
      new SaveDeferralReport.SlaDeferredReport(CPT2, DeferralType.NOT_DEFERRED)
  );


  @Autowired
  private MockMvc mvc;

  @MockBean
  private SaveDeferralReport saveDeferralReport;

  private static Stream<Arguments> parametersSaveStatus() {
    return Stream.of(
        Arguments.of(URL_FILE, status().isCreated(), 1),
        Arguments.of(URL_FILE_BAD_REQUEST, status().isBadRequest(), 0),
        Arguments.of(URL_FILE_BAD_REQUEST_NULL, status().isBadRequest(), 0),
        Arguments.of(URL_FILE_BAD_REQUEST_NOT_OBJECT, status().isBadRequest(), 0)
    );
  }

  @ParameterizedTest
  @MethodSource("parametersSaveStatus")
  public void saveTest(final String url, final ResultMatcher statusController, final int times) throws Exception {
    //WHEN
    final ResultActions result = mvc.perform(
        post(URL)
            .contentType(APPLICATION_JSON)
            .content(getResourceAsString(url))
    );

    // THEN
    result.andExpect(statusController);

    verify(saveDeferralReport, times(times)).save(LOGISTIC_CENTER_ID, NOW, SLA_DEFERRED);
  }

}
