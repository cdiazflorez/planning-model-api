package com.mercadolibre.planning.model.api.domain.usecase.deferral;

import static com.mercadolibre.planning.model.api.util.DateUtils.getCurrentUtcDate;

import com.mercadolibre.planning.model.api.web.controller.deferral.DeferralResponse;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class SaveOutboundDeferralReport {

  private static final int PURGE_HOURS_RANGE = 96;

  private static final String SUCCESS_CPT_REPORT_MSG = "Saved CptDeferralReport for %s with deferral date: %s";

  private static final String BAD_REQUEST_CPT_REPORT_MSG =
      "Something went wrong, the given deferralDate is after current date for %s with deferral date: %s";

  private static final String FAIL_CPT_REPORT_MSG =
      "Something went wrong while saving data, for %s with deferral date: %s";

  private DeferralReportGateway deferralReportGateway;

  /**
   * Follows these steps to save deferral report.
   * Delete old registers using currentDate minus {@link #PURGE_HOURS_RANGE} hours.
   * Get the last CptDeferralReport.
   * Update the CptDeferralReport using lastReport and newReport, filtering expired CPTs.
   * Save updated CptDeferralReport.
   *
   * @param logisticCenterId     logistic center of report
   * @param deferralDate         date call api
   * @param newCptDeferralReport list of class contain cpt date, isDeferredOn and reason of deferred (cap max or cascade)
   * @return DeferralResponse based on saving process to define its status code and message.
   */
  public DeferralResponse save(
      final String logisticCenterId,
      final Instant deferralDate,
      final List<CptDeferralReport> newCptDeferralReport
  ) {

    final Instant currentDate = getCurrentUtcDate().toInstant();
    if (deferralDate.isAfter(currentDate)) {
      return new DeferralResponse(
          HttpStatus.BAD_REQUEST.value(),
          String.format(BAD_REQUEST_CPT_REPORT_MSG, logisticCenterId, deferralDate)
      );
    }

    try {
      deleteDeferralReport(logisticCenterId, deferralDate);

      final List<CptDeferralReport> lastCptDeferralReport =
          deferralReportGateway.getLastCptDeferralReportForLogisticCenter(logisticCenterId);

      final List<CptDeferralReport> updatedCptDeferralReport = updateCptDeferralReport(
          lastCptDeferralReport,
          newCptDeferralReport,
          currentDate
      );
      log.info(SaveDeferralReportLogger.generateLogUpdate(logisticCenterId, deferralDate));
      deferralReportGateway.saveDeferralReport(logisticCenterId, deferralDate, updatedCptDeferralReport);

      return new DeferralResponse(
          HttpStatus.CREATED.value(),
          String.format(SUCCESS_CPT_REPORT_MSG, logisticCenterId, deferralDate)
      );

    } catch (DataAccessException exception) {
      log.error(String.format(FAIL_CPT_REPORT_MSG, logisticCenterId, deferralDate));

      return new DeferralResponse(
          HttpStatus.INTERNAL_SERVER_ERROR.value(),
          String.format(FAIL_CPT_REPORT_MSG, logisticCenterId, deferralDate)
      );
    }
  }

  /**
   * Deletes deferral reports for the specified logistic center and date.
   *
   * @param logisticCenterId The ID of the logistic center for which deferral reports will be deleted.
   * @param date             The reference date for deleting deferral reports.
   */
  private void deleteDeferralReport(final String logisticCenterId, final Instant date) {
    final Instant dateToDelete = date.minus(PURGE_HOURS_RANGE, ChronoUnit.HOURS);

    try {
      final int deletedRegisters = deferralReportGateway.deleteDeferralReportBeforeDate(dateToDelete);
      log.info(SaveDeferralReportLogger.generateLogMessage(deletedRegisters, logisticCenterId, dateToDelete, date));
    } catch (SQLException sqlException) {
      log.error(SaveDeferralReportLogger.generateLogError(date, sqlException.getMessage()));
    }
  }

  /**
   * Updates the CptDeferralReport list by merging the last and new deferral reports.
   *
   * @param lastCptDeferralReport The list of CptDeferralReport representing the last deferral reports.
   * @param newCptDeferralReport  The list of CptDeferralReport representing the new deferral reports.
   * @return The updated list of CptDeferralReport after merging and sorting by date.
   */
  private List<CptDeferralReport> updateCptDeferralReport(
      final List<CptDeferralReport> lastCptDeferralReport,
      final List<CptDeferralReport> newCptDeferralReport,
      final Instant currentDate
  ) {

    if (lastCptDeferralReport.isEmpty()) {
      return newCptDeferralReport;
    }

    return mapCptDeferralReportsToMergedCptReportByDate(lastCptDeferralReport, newCptDeferralReport, currentDate).values().stream()
        .sorted(Comparator.comparing(CptDeferralReport::getDate))
        .collect(Collectors.toList());
  }

  /**
   * Maps the CptDeferralReports to a merged CptDeferralReport by date.
   * Updates lastCptDeferralReport-updated to false and replace existing with the newCptDeferralReport.
   * Also filters the new report removing expired CPTs.
   *
   * @param lastCptDeferralReport The list of CptDeferralReport representing the last deferral reports.
   * @param newCptDeferralReport  The list of CptDeferralReport representing the new deferral reports.
   * @return A map with key representing the date and the merged CptDeferralReport as the value.
   */
  private Map<Instant, CptDeferralReport> mapCptDeferralReportsToMergedCptReportByDate(
      final List<CptDeferralReport> lastCptDeferralReport,
      final List<CptDeferralReport> newCptDeferralReport,
      final Instant currentDate
  ) {
    return Stream.concat(
            streamCptDeferralReportAsFalse(lastCptDeferralReport),
            newCptDeferralReport.stream()
        )
        .filter(cptDeferralReport -> cptDeferralReport.date.isAfter(currentDate))
        .collect(Collectors.toMap(CptDeferralReport::getDate, cptReport -> cptReport, (oldReport, newReport) -> newReport));
  }

  /**
   * Streams the CptDeferralReports from the last deferral report list as false for the "updated" field.
   *
   * @param lastCptDeferralReport The list of CptDeferralReport representing the last deferral reports.
   * @return A stream of CptDeferralReport objects with the "updated" field set to false.
   */
  private Stream<CptDeferralReport> streamCptDeferralReportAsFalse(final List<CptDeferralReport> lastCptDeferralReport) {
    return lastCptDeferralReport.stream()
        .map(cptReport -> new CptDeferralReport(cptReport.getDate(), false, cptReport.getStatus()));
  }

  /**
   * Deferral gateway to delete and save reports.
   */
  public interface DeferralReportGateway {

    /**
     * Retrieves the last CptDeferralReport list for the specified logistic center.
     *
     * @param logisticCenter The ID or name of the logistic center.
     * @return A list of CptDeferralReport or empty List.
     */
    List<CptDeferralReport> getLastCptDeferralReportForLogisticCenter(String logisticCenter);

    /**
     * Delete deferral report before dateTo of param.
     *
     * @param dateTo date to begin delete in inverse order
     * @return number of records deleted
     * @throws SQLException throws it, if deletedDeferral crashed
     */
    int deleteDeferralReportBeforeDate(Instant dateTo) throws SQLException;

    /**
     * Save deferral report when CAP 5 is on or off.
     *
     * @param logisticCenterId logistic center id
     * @param date             date on or off
     * @param cptDeferrals     list with cpt information to save
     */
    void saveDeferralReport(String logisticCenterId, Instant date, List<CptDeferralReport> cptDeferrals);
  }

  @AllArgsConstructor
  @Value
  public static class CptDeferralReport {
    Instant date;

    boolean updated;

    DeferralType status;
  }

}
