package com.mercadolibre.planning.model.api.domain.usecase.deferral;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class SaveDeferralReport {

  private static final int PURGE_HOURS_RANGE = 48;

  private static final int NO_REGISTER_DELETED = 0;

  private DeferralGateway deferralGateway;

  /**
   * Save deferral report.
   * First deleted old registers reports that were before {@link #PURGE_HOURS_RANGE} hours.
   * If there are any register deleted will log its.
   * After deleted old registers, save new registers reported.
   *
   * @param logisticCenterId logistic center of report
   * @param date             date operation
   * @param slas             list of class contain sla date, isDeferredOn and reason of deferred (cap max or cascade)
   * @param viewDate         date call api
   * @return result of save report
   */
  public HttpStatus save(final String logisticCenterId, final Instant date, final List<SlaDeferred> slas, final Instant viewDate) {

    deleteDeferralReport(logisticCenterId, viewDate);

    return deferralGateway.saveDeferralReport(logisticCenterId, date, slas);
  }

  private void deleteDeferralReport(final String logisticCenterId, final Instant viewDate) {
    final Instant dateToDelete = viewDate.minus(PURGE_HOURS_RANGE, ChronoUnit.HOURS);

    try {
      final int deletedRegisters = deferralGateway.deleteDeferralReportBeforeDate(dateToDelete);

      if (deletedRegisters > NO_REGISTER_DELETED) {
        log.info(SaveDeferralReportLogger.generateLogMessage(deletedRegisters, logisticCenterId, dateToDelete, viewDate));
      }
    } catch (SQLException sqlException) {
      log.error(SaveDeferralReportLogger.generateLogError(viewDate, sqlException.getMessage()));
    }
  }

  /**
   * Deferral gateway to delete and save reports.
   */
  public interface DeferralGateway {
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
     * @param slas             list with sla information to save
     * @return status result of save report
     */
    HttpStatus saveDeferralReport(String logisticCenterId, Instant date, List<SlaDeferred> slas);
  }

  @AllArgsConstructor
  @Value
  public static class SlaDeferred {
    Instant date;
    boolean isDeferredOn;
    DeferralType reason;
  }

}
