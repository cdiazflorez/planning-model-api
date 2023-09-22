package com.mercadolibre.planning.model.api.domain.usecase.deferral;

import static com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType.CAP_MAX;
import static com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType.CASCADE;
import static com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType.NOT_DEFERRED;
import static com.mercadolibre.planning.model.api.util.DateUtils.getCurrentUtcDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.usecase.deferral.SaveOutboundDeferralReport.CptDeferralReport;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.SaveOutboundDeferralReport.DeferralReportGateway;
import com.mercadolibre.planning.model.api.web.controller.deferral.DeferralResponse;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class SaveOutboundDeferralReportTest {

  private static final int PURGE_HOURS_RANGE = 96;

  private static final String LOGISTIC_CENTER_ID = "ARBA01";

  private static final Instant LOGGER_DATE = Instant.parse("2023-09-28T11:00:00Z");

  private static final Instant PURGE_LOGGER_DATE = LOGGER_DATE.minus(PURGE_HOURS_RANGE, ChronoUnit.HOURS);

  private static final Instant DEFERRAL_DATE_NOW = getCurrentUtcDate().toInstant();

  private static final Instant PURGE_DEFERRAL_DATE = DEFERRAL_DATE_NOW.minus(PURGE_HOURS_RANGE, ChronoUnit.HOURS);

  private static final Instant CPT_1_DATE = DEFERRAL_DATE_NOW.plus(6, ChronoUnit.HOURS);

  private static final CptDeferralReport CPT_1_DEFERRAL_ON_CAP_MAX = new CptDeferralReport(CPT_1_DATE, true, CAP_MAX);

  private static final CptDeferralReport CPT_1_DEFERRAL_ON_NOT_DEFERRED = new CptDeferralReport(CPT_1_DATE, true, NOT_DEFERRED);

  private static final CptDeferralReport CPT_EXPIRED_ON_CASCADE = new CptDeferralReport(
      DEFERRAL_DATE_NOW.minus(1, ChronoUnit.HOURS),
      true,
      CASCADE
  );

  private static final Instant CPT_2_DATE = DEFERRAL_DATE_NOW.plus(7, ChronoUnit.HOURS);

  private static final CptDeferralReport CPT_2_DEFERRAL_ON_CASCADE = new CptDeferralReport(CPT_2_DATE, true, CASCADE);

  private static final CptDeferralReport CPT_2_DEFERRAL_ON_NOT_DEFERRED = new CptDeferralReport(CPT_2_DATE, true, NOT_DEFERRED);

  private static final List<CptDeferralReport> NEW_CPT_DEFERRAL_REPORT =
      List.of(CPT_1_DEFERRAL_ON_NOT_DEFERRED, CPT_2_DEFERRAL_ON_NOT_DEFERRED);

  private static final Instant CPT_3_DATE = DEFERRAL_DATE_NOW.plus(8, ChronoUnit.HOURS);

  private static final CptDeferralReport CPT_3_DEFERRAL_ON_CASCADE = new CptDeferralReport(CPT_3_DATE, true, CASCADE);

  private static final List<CptDeferralReport> NEW_CPT_DEFERRAL_REPORT_2 = List.of(CPT_1_DEFERRAL_ON_CAP_MAX, CPT_3_DEFERRAL_ON_CASCADE);

  private static final CptDeferralReport CPT_3_DEFERRAL_OFF_CASCADE = new CptDeferralReport(CPT_3_DATE, false, CASCADE);

  private static final Instant CPT_4_DATE = DEFERRAL_DATE_NOW.plus(9, ChronoUnit.HOURS);

  private static final CptDeferralReport CPT_4_DEFERRAL_ON_NOT_DEFERRED = new CptDeferralReport(CPT_4_DATE, true, CASCADE);

  private static final List<CptDeferralReport> LAST_CPT_DEFERRAL_REPORT = List.of(
      CPT_1_DEFERRAL_ON_CAP_MAX,
      CPT_2_DEFERRAL_ON_CASCADE,
      CPT_3_DEFERRAL_ON_CASCADE,
      CPT_4_DEFERRAL_ON_NOT_DEFERRED
  );

  private static final CptDeferralReport CPT_4_DEFERRAL_OFF_NOT_DEFERRED = new CptDeferralReport(CPT_4_DATE, false, CASCADE);

  private static final List<CptDeferralReport> UPDATED_CPT_DEFERRAL_REPORT = List.of(
      CPT_1_DEFERRAL_ON_NOT_DEFERRED,
      CPT_2_DEFERRAL_ON_NOT_DEFERRED,
      CPT_3_DEFERRAL_OFF_CASCADE,
      CPT_4_DEFERRAL_OFF_NOT_DEFERRED
  );

  private static final String SUCCESS_CPT_REPORT_MSG = "Saved CptDeferralReport for %s with deferral date: %s";

  private static final String BAD_REQUEST_CPT_REPORT_MSG =
      "Something went wrong, the given deferralDate is after current date for %s with deferral date: %s";

  private static final String INTERNAL_ERROR_CPT_REPORT_MSG =
      "Something went wrong while saving data, for %s with deferral date: %s";

  private static final int DELETED_ONE_REGISTER = 1;

  private static final int NO_DELETED_REGISTER = 0;

  private static final int EXPECTED_INVOCATION_TIMES_NONE = 0;

  private static final int EXPECTED_INVOCATION_TIMES_ONE = 1;

  private static final String MESSAGE_LOG =
      "Deleted %s sla Deferred report register of logisticCenter ARBA01, with dateTo = 2023-09-24T11:00:00Z to 2023-09-28T11:00:00Z hs";

  private static final String MESSAGE_ERROR_SQL = "Error deleted sql";

  private static final String MESSAGE_LOGGER = String.format(MESSAGE_LOG, DELETED_ONE_REGISTER);

  private static final String MESSAGE_ERROR_DELETE = String.format("Deleted view_date = 2023-09-28T11:00:00Z. %s", MESSAGE_ERROR_SQL);


  @InjectMocks
  private SaveOutboundDeferralReport saveOutboundDeferralReport;

  @Mock
  private DeferralReportGateway deferralReportGateway;


  private static Stream<Arguments> parametersTestSave() {
    return Stream.of(
        Arguments.of(
            LOGISTIC_CENTER_ID,
            DEFERRAL_DATE_NOW,
            NEW_CPT_DEFERRAL_REPORT,
            PURGE_DEFERRAL_DATE,
            DELETED_ONE_REGISTER,
            LAST_CPT_DEFERRAL_REPORT,
            UPDATED_CPT_DEFERRAL_REPORT,
            EXPECTED_INVOCATION_TIMES_ONE,
            HttpStatus.CREATED.value(),
            SUCCESS_CPT_REPORT_MSG
        ),
        Arguments.of(
            LOGISTIC_CENTER_ID,
            DEFERRAL_DATE_NOW,
            NEW_CPT_DEFERRAL_REPORT_2,
            PURGE_DEFERRAL_DATE,
            NO_DELETED_REGISTER,
            List.of(),
            NEW_CPT_DEFERRAL_REPORT_2,
            EXPECTED_INVOCATION_TIMES_ONE,
            HttpStatus.CREATED.value(),
            SUCCESS_CPT_REPORT_MSG
        ),
        Arguments.of(
            LOGISTIC_CENTER_ID,
            DEFERRAL_DATE_NOW,
            List.of(CPT_1_DEFERRAL_ON_CAP_MAX, CPT_2_DEFERRAL_ON_CASCADE),
            PURGE_DEFERRAL_DATE,
            NO_DELETED_REGISTER,
            List.of(CPT_3_DEFERRAL_ON_CASCADE, CPT_4_DEFERRAL_ON_NOT_DEFERRED),
            List.of(CPT_1_DEFERRAL_ON_CAP_MAX, CPT_2_DEFERRAL_ON_CASCADE, CPT_3_DEFERRAL_OFF_CASCADE, CPT_4_DEFERRAL_OFF_NOT_DEFERRED),
            EXPECTED_INVOCATION_TIMES_ONE,
            HttpStatus.CREATED.value(),
            SUCCESS_CPT_REPORT_MSG
        ),
        Arguments.of(
            LOGISTIC_CENTER_ID,
            DEFERRAL_DATE_NOW,
            List.of(CPT_1_DEFERRAL_ON_CAP_MAX, CPT_2_DEFERRAL_ON_CASCADE),
            PURGE_DEFERRAL_DATE,
            NO_DELETED_REGISTER,
            List.of(CPT_EXPIRED_ON_CASCADE, CPT_4_DEFERRAL_ON_NOT_DEFERRED),
            List.of(CPT_1_DEFERRAL_ON_CAP_MAX, CPT_2_DEFERRAL_ON_CASCADE, CPT_4_DEFERRAL_OFF_NOT_DEFERRED),
            EXPECTED_INVOCATION_TIMES_ONE,
            HttpStatus.CREATED.value(),
            SUCCESS_CPT_REPORT_MSG
        )
    );
  }

  private static Stream<Arguments> parametersTestSaveDeferralAfterNow() {
    return Stream.of(
        Arguments.of(
            LOGISTIC_CENTER_ID,
            DEFERRAL_DATE_NOW.plus(2, ChronoUnit.HOURS),
            List.of(CPT_1_DEFERRAL_ON_CAP_MAX, CPT_2_DEFERRAL_ON_CASCADE),
            EXPECTED_INVOCATION_TIMES_NONE,
            HttpStatus.BAD_REQUEST.value(),
            BAD_REQUEST_CPT_REPORT_MSG
        )
    );
  }

  private static Stream<Arguments> parametersTestSaveError() {
    return Stream.of(
        Arguments.of(
            LOGISTIC_CENTER_ID,
            DEFERRAL_DATE_NOW,
            List.of(CPT_1_DEFERRAL_ON_CAP_MAX, CPT_2_DEFERRAL_ON_CASCADE),
            new QueryTimeoutException(INTERNAL_ERROR_CPT_REPORT_MSG),
            EXPECTED_INVOCATION_TIMES_NONE,
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            INTERNAL_ERROR_CPT_REPORT_MSG
        )
    );
  }

  private static Stream<Arguments> parametersTestsDeletedException() {
    return Stream.of(
        Arguments.of(
            LOGISTIC_CENTER_ID,
            DEFERRAL_DATE_NOW,
            NEW_CPT_DEFERRAL_REPORT,
            PURGE_DEFERRAL_DATE,
            new SQLException(MESSAGE_ERROR_SQL),
            LAST_CPT_DEFERRAL_REPORT,
            UPDATED_CPT_DEFERRAL_REPORT
        )
    );
  }

  private static DeferralResponse mockDeferralResponse(final int status, final String msg) {
    return new DeferralResponse(status, msg);
  }

  @ParameterizedTest
  @MethodSource("parametersTestSave")
  @DisplayName(
      "1: new CPT report with existing CPTs into last CPT report; "
          + "2: new CPT report when empty last CPTs; "
          + "3: new CPT report with none existing CPTs into last CPT report; "
          + "4: new CPT report with expired existing CPTs into last CPT report; "
  )
  void saveTest(
      final String logisticCenterId,
      final Instant deferralDate,
      final List<CptDeferralReport> newCptDeferralReports,
      final Instant dateToDelete,
      final int deletedRegisters,
      final List<CptDeferralReport> lastCptDeferralReportList,
      final List<CptDeferralReport> updatedCptDeferralReportList,
      final int wantedNumberOfInvocationsOfSave,
      final int expectedStatus,
      final String responseMsg
  ) throws SQLException {

    //GIVEN
    final String expectedMsg = String.format(responseMsg, logisticCenterId, deferralDate);
    final DeferralResponse expectedDeferralResponse = mockDeferralResponse(expectedStatus, expectedMsg);

    when(deferralReportGateway.deleteDeferralReportBeforeDate(dateToDelete))
        .thenReturn(deletedRegisters);

    //WHEN
    final DeferralResponse deferralResponse = saveOutboundDeferralReport.save(logisticCenterId, deferralDate, newCptDeferralReports);

    //THEN
    verify(deferralReportGateway, times(wantedNumberOfInvocationsOfSave))
        .saveDeferralReport(logisticCenterId, deferralDate, newCptDeferralReports);

    assertEquals(expectedDeferralResponse.getStatus(), deferralResponse.getStatus());
    assertEquals(expectedDeferralResponse.getMessage(), deferralResponse.getMessage());
  }

  @ParameterizedTest
  @MethodSource("parametersTestSaveDeferralAfterNow")
  @DisplayName("new CPT report with deferral date after current date")
  void saveDeferralAfterNowTest(
      final String logisticCenterId,
      final Instant deferralDate,
      final List<CptDeferralReport> newCptDeferralReports,
      final int wantedNumberOfInvocationsOfSave,
      final int expectedStatus,
      final String responseMsg
  ) {

    //GIVEN
    final String expectedMsg = String.format(responseMsg, logisticCenterId, deferralDate);
    final DeferralResponse expectedDeferralResponse = mockDeferralResponse(expectedStatus, expectedMsg);

    //WHEN
    final DeferralResponse deferralResponse = saveOutboundDeferralReport.save(logisticCenterId, deferralDate, newCptDeferralReports);

    //THEN
    verify(deferralReportGateway, times(wantedNumberOfInvocationsOfSave))
        .saveDeferralReport(any(), any(), anyList());

    assertEquals(expectedDeferralResponse.getStatus(), deferralResponse.getStatus());
    assertEquals(expectedDeferralResponse.getMessage(), deferralResponse.getMessage());
  }

  @ParameterizedTest
  @MethodSource("parametersTestsDeletedException")
  void saveTestDeletedException(
      final String logisticCenterId,
      final Instant deferralDate,
      final List<SaveOutboundDeferralReport.CptDeferralReport> newCptDeferralReports,
      final Instant dateToDelete,
      final SQLException exception,
      final List<CptDeferralReport> lastCptDeferralReportList,
      final List<CptDeferralReport> updatedCptDeferralReportList
  ) throws SQLException {

    //GIVEN
    when(deferralReportGateway.deleteDeferralReportBeforeDate(dateToDelete))
        .thenThrow(exception);

    //WHEN
    saveOutboundDeferralReport.save(logisticCenterId, deferralDate, newCptDeferralReports);

    //THEN
    verify(deferralReportGateway, times(1))
        .saveDeferralReport(logisticCenterId, deferralDate, newCptDeferralReports);
  }

  @Test
  void testLogger() {
    final String errorDelete = SaveDeferralReportLogger.generateLogError(LOGGER_DATE, MESSAGE_ERROR_SQL);
    final String logger =
        SaveDeferralReportLogger.generateLogMessage(DELETED_ONE_REGISTER, LOGISTIC_CENTER_ID, PURGE_LOGGER_DATE, LOGGER_DATE);

    assertEquals(MESSAGE_LOGGER, logger);
    assertEquals(MESSAGE_ERROR_DELETE, errorDelete);
  }
}
