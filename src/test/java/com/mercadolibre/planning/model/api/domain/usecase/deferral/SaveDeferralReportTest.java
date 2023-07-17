package com.mercadolibre.planning.model.api.domain.usecase.deferral;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class SaveDeferralReportTest {

  private static final String LOGISTIC_CENTER_ID = "ARBA01";

  private static final Instant VIEW_DATE = Instant.parse("2023-06-28T11:00:00Z");

  private static final Instant VIEW_DATE_BEFORE = Instant.parse("2023-06-26T11:00:00Z");

  private static final Instant OPERATION_DATE_2 = Instant.parse("2023-06-28T10:20:00Z");

  private static final Instant SLA_1 = Instant.parse("2023-06-28T10:00:00Z");

  private static final SaveDeferralReport.SlaDeferred SLA_DEFERRAL_ON_CAP_MAX =
      new SaveDeferralReport.SlaDeferred(SLA_1, true, DeferralType.CAP_MAX);

  private static final SaveDeferralReport.SlaDeferred SLA_DEFERRAL_ON_CASCADE =
      new SaveDeferralReport.SlaDeferred(SLA_1, true, DeferralType.CASCADE);

  private static final SaveDeferralReport.SlaDeferred SLA_NO_DEFERRAL_OFF = new SaveDeferralReport.SlaDeferred(SLA_1, false, null);

  private static final int DELETED_ONE_REGISTER = 1;

  private static final int NO_DELETED_REGISTER = 0;

  private static final String MESSAGE_LOG =
      "Deleted %s sla Deferred report register of logisticCenter ARBA01, with dateTo = 2023-06-26T11:00:00Z to 2023-06-28T11:00:00Z hs";

  private static final String MESSAGE_ERROR_SQL = "Error deleted sql";

  private static final String MESSAGE_LOGGER = String.format(MESSAGE_LOG, DELETED_ONE_REGISTER);

  private static final String MESSAGE_ERROR_DELETE = String.format("Deleted view_date = 2023-06-28T11:00:00Z. %s", MESSAGE_ERROR_SQL);


  @InjectMocks
  private SaveDeferralReport saveDeferralReport;

  @Mock
  private SaveDeferralReport.DeferralGateway deferralGateway;


  private static Stream<Arguments> parametersTestSave() {
    return Stream.of(
        Arguments.of(LOGISTIC_CENTER_ID, VIEW_DATE_BEFORE, List.of(SLA_DEFERRAL_ON_CAP_MAX, SLA_DEFERRAL_ON_CASCADE), DELETED_ONE_REGISTER,
            VIEW_DATE_BEFORE, HttpStatus.OK),
        Arguments.of(LOGISTIC_CENTER_ID, OPERATION_DATE_2, List.of(SLA_NO_DEFERRAL_OFF), DELETED_ONE_REGISTER, VIEW_DATE_BEFORE,
            HttpStatus.OK),

        Arguments.of(LOGISTIC_CENTER_ID, VIEW_DATE_BEFORE, List.of(SLA_DEFERRAL_ON_CAP_MAX, SLA_DEFERRAL_ON_CASCADE), NO_DELETED_REGISTER,
            VIEW_DATE_BEFORE, HttpStatus.OK),
        Arguments.of(LOGISTIC_CENTER_ID, OPERATION_DATE_2, List.of(SLA_NO_DEFERRAL_OFF), NO_DELETED_REGISTER, VIEW_DATE_BEFORE,
            HttpStatus.OK)
    );
  }

  private static Stream<Arguments> parametersTestsDeletedException() {
    return Stream.of(
        Arguments.of(LOGISTIC_CENTER_ID, VIEW_DATE_BEFORE, List.of(SLA_DEFERRAL_ON_CAP_MAX, SLA_DEFERRAL_ON_CASCADE), VIEW_DATE_BEFORE,
            HttpStatus.OK, new SQLException(MESSAGE_ERROR_SQL))
    );
  }

  @ParameterizedTest
  @MethodSource("parametersTestSave")
  void saveTest(
      final String logisticCenterId, final Instant date, final List<SaveDeferralReport.SlaDeferred> slas, final int deletedRegisters,
      final Instant dateToBefore, final HttpStatus status) throws SQLException {

    when(deferralGateway.deleteDeferralReportBeforeDate(dateToBefore))
        .thenReturn(deletedRegisters);

    when(saveDeferralReport.save(logisticCenterId, date, slas, VIEW_DATE))
        .thenReturn(status);

    final var statusResponse = saveDeferralReport.save(logisticCenterId, date, slas, VIEW_DATE);

    assertEquals(status, statusResponse);
  }

  @ParameterizedTest
  @MethodSource("parametersTestsDeletedException")
  void saveTestDeletedException(
      final String logisticCenterId, final Instant date, final List<SaveDeferralReport.SlaDeferred> slas, final Instant dateToBefore,
      final HttpStatus status, final SQLException exception) throws SQLException {

    when(deferralGateway.deleteDeferralReportBeforeDate(dateToBefore))
        .thenThrow(exception);

    when(saveDeferralReport.save(logisticCenterId, date, slas, VIEW_DATE))
        .thenReturn(status);

    final var statusResponse = saveDeferralReport.save(logisticCenterId, date, slas, VIEW_DATE);

    assertEquals(status, statusResponse);
  }

  @Test
  void testLogger() {
    final String errorDelete = SaveDeferralReportLogger.generateLogError(VIEW_DATE, MESSAGE_ERROR_SQL);
    final String logger =
        SaveDeferralReportLogger.generateLogMessage(DELETED_ONE_REGISTER, LOGISTIC_CENTER_ID, VIEW_DATE_BEFORE, VIEW_DATE);

    assertEquals(MESSAGE_LOGGER, logger);
    assertEquals(MESSAGE_ERROR_DELETE, errorDelete);
  }
}
