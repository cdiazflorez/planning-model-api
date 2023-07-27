package com.mercadolibre.planning.model.api.domain.usecase.deferral;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class SaveDeferralReport {

  private static final int PURGE_HOURS_RANGE = 96;

  private static final int NO_REGISTER_DELETED = 0;

  private DeferralReportGateway deferralReportGateway;

  /**
   * Save deferral report.
   * First deleted old registers reports that were before {@link #PURGE_HOURS_RANGE} hours.
   * If there are any register deleted will log its.
   * After deleted old registers, save new registers reported.
   *
   * @param logisticCenterId   logistic center of report
   * @param date               date call api
   * @param slaDeferredReports list of class contain sla date, isDeferredOn and reason of deferred (cap max or cascade)
   */
  public void save(
      final String logisticCenterId,
      final Instant date,
      final List<SlaDeferredReport> slaDeferredReports
  ) {

    deleteDeferralReport(logisticCenterId, date);

    deferralReportGateway.saveDeferralReport(logisticCenterId, date, mapCptDeferred(slaDeferredReports));
  }

  private void deleteDeferralReport(final String logisticCenterId, final Instant date) {
    final Instant dateToDelete = date.minus(PURGE_HOURS_RANGE, ChronoUnit.HOURS);

    try {
      final int deletedRegisters = deferralReportGateway.deleteDeferralReportBeforeDate(dateToDelete);

      if (deletedRegisters > NO_REGISTER_DELETED) {
        log.info(SaveDeferralReportLogger.generateLogMessage(deletedRegisters, logisticCenterId, dateToDelete, date));
      }
    } catch (SQLException sqlException) {
      log.error(SaveDeferralReportLogger.generateLogError(date, sqlException.getMessage()));
    }
  }

  //TODO: add logic if update is true or false, here and in its test
  private List<CptDeferred> mapCptDeferred(final List<SlaDeferredReport> slaDeferredReports) {
    return slaDeferredReports.stream().map(
        slaDeferredReport -> new CptDeferred(slaDeferredReport.getDate(), false, slaDeferredReport.getStatus())
    ).collect(Collectors.toList());
  }

  /**
   * Deferral gateway to delete and save reports.
   */
  public interface DeferralReportGateway {
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
     * @param cpt              list with cpt information to save
     */
    void saveDeferralReport(String logisticCenterId, Instant date, List<CptDeferred> cpt);
  }

  @AllArgsConstructor
  @Value
  public static class CptDeferred {
    Instant date;

    boolean updated;

    DeferralType status;
  }

  @AllArgsConstructor
  @Value
  public static class SlaDeferredReport {
    Instant date;

    DeferralType status;
  }

}
