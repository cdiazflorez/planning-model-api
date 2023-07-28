package com.mercadolibre.planning.model.api.domain.usecase.deferral;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.usecase.deferral.SaveDeferralReport.CptDeferred;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.SaveDeferralReport.DeferralReportGateway;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

@ExtendWith(MockitoExtension.class)
class SaveDeferralReportTest {

  private static final int PURGE_HOURS_RANGE = 96;

  private static final String LOGISTIC_CENTER_ID = "ARBA01";

  private static final Instant VIEW_DATE = Instant.parse("2023-06-28T11:00:00Z");

  private static final Instant VIEW_DATE_BEFORE = Instant.parse("2023-06-26T11:00:00Z");

  private static final Instant PURGE_VIEW_DATE_BEFORE = getDateToDelete(VIEW_DATE_BEFORE);

  private static final Instant OPERATION_DATE_2 = Instant.parse("2023-06-28T10:20:00Z");

  private static final Instant PURGE_OPERATION_DATE_2 = getDateToDelete(OPERATION_DATE_2);

  private static final Instant SLA_1 = Instant.parse("2023-06-28T10:00:00Z");

  private static final SaveDeferralReport.SlaDeferredReport SLA_DEFERRAL_ON_CAP_MAX =
      new SaveDeferralReport.SlaDeferredReport(SLA_1, DeferralType.CAP_MAX);

  private static final SaveDeferralReport.SlaDeferredReport SLA_DEFERRAL_ON_CASCADE =
      new SaveDeferralReport.SlaDeferredReport(SLA_1, DeferralType.CASCADE);

  private static final SaveDeferralReport.SlaDeferredReport SLA_NO_DEFERRAL_OFF =
      new SaveDeferralReport.SlaDeferredReport(SLA_1, DeferralType.NOT_DEFERRED);


  private static final CptDeferred CPT_DEFERRAL_ON_CAP_MAX = new CptDeferred(SLA_1, false, DeferralType.CAP_MAX);

  private static final CptDeferred CPT_DEFERRAL_ON_CASCADE = new CptDeferred(SLA_1, false, DeferralType.CASCADE);

  private static final CptDeferred CPT_NO_DEFERRAL_OFF = new CptDeferred(SLA_1, false, DeferralType.NOT_DEFERRED);

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
  private DeferralReportGateway deferralReportGateway;


  private static Stream<Arguments> parametersTestSave() {
    return Stream.of(
        Arguments.of(
            LOGISTIC_CENTER_ID,
            VIEW_DATE_BEFORE,
            PURGE_VIEW_DATE_BEFORE,
            List.of(SLA_DEFERRAL_ON_CAP_MAX, SLA_DEFERRAL_ON_CASCADE),
            DELETED_ONE_REGISTER,
            List.of(CPT_DEFERRAL_ON_CAP_MAX, CPT_DEFERRAL_ON_CASCADE)
        ),
        Arguments.of(
            LOGISTIC_CENTER_ID,
            OPERATION_DATE_2,
            PURGE_OPERATION_DATE_2,
            List.of(SLA_NO_DEFERRAL_OFF),
            DELETED_ONE_REGISTER,
            List.of(CPT_NO_DEFERRAL_OFF)
        ),
        Arguments.of(
            LOGISTIC_CENTER_ID,
            VIEW_DATE_BEFORE,
            PURGE_VIEW_DATE_BEFORE,
            List.of(SLA_DEFERRAL_ON_CAP_MAX, SLA_DEFERRAL_ON_CASCADE),
            NO_DELETED_REGISTER,
            List.of(CPT_DEFERRAL_ON_CAP_MAX, CPT_DEFERRAL_ON_CASCADE)
        ),
        Arguments.of(
            LOGISTIC_CENTER_ID,
            OPERATION_DATE_2,
            PURGE_OPERATION_DATE_2,
            List.of(SLA_NO_DEFERRAL_OFF),
            NO_DELETED_REGISTER,
            List.of(CPT_NO_DEFERRAL_OFF)
        )
    );
  }

  private static Instant getDateToDelete(final Instant date) {
    return date.minus(PURGE_HOURS_RANGE, ChronoUnit.HOURS);
  }

  private static Stream<Arguments> parametersTestsDeletedException() {
    return Stream.of(
        Arguments.of(
            LOGISTIC_CENTER_ID,
            VIEW_DATE_BEFORE,
            PURGE_VIEW_DATE_BEFORE,
            List.of(SLA_DEFERRAL_ON_CAP_MAX, SLA_DEFERRAL_ON_CASCADE),
            new SQLException(MESSAGE_ERROR_SQL),
            List.of(CPT_DEFERRAL_ON_CAP_MAX, CPT_DEFERRAL_ON_CASCADE)
        )
    );
  }

  @ParameterizedTest
  @MethodSource("parametersTestSave")
  void saveTest(
      final String logisticCenterId,
      final Instant deferralDate,
      final Instant dateToDelete,
      final List<SaveDeferralReport.SlaDeferredReport> slaDeferredReports,
      final int deletedRegisters,
      final List<CptDeferred> cptDeferredList
  ) throws SQLException {

    //GIVEN
    when(deferralReportGateway.deleteDeferralReportBeforeDate(dateToDelete))
        .thenReturn(deletedRegisters);

    //WHEN
    saveDeferralReport.save(logisticCenterId, deferralDate, slaDeferredReports);

    //THEN
    verify(deferralReportGateway, times(1)).saveDeferralReport(logisticCenterId, deferralDate, cptDeferredList);
  }

  @ParameterizedTest
  @MethodSource("parametersTestsDeletedException")
  void saveTestDeletedException(
      final String logisticCenterId,
      final Instant deferralDate,
      final Instant dateToDelete,
      final List<SaveDeferralReport.SlaDeferredReport> slaDeferredReport,
      final SQLException exception,
      final List<CptDeferred> cptDeferredList
  ) throws SQLException {

    //GIVEN
    when(deferralReportGateway.deleteDeferralReportBeforeDate(dateToDelete))
        .thenThrow(exception);

    //WHEN
    saveDeferralReport.save(logisticCenterId, deferralDate, slaDeferredReport);

    //THEN
    verify(deferralReportGateway, times(1)).saveDeferralReport(logisticCenterId, deferralDate, cptDeferredList);
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
