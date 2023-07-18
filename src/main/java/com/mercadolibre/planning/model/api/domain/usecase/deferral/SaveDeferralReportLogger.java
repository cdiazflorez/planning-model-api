package com.mercadolibre.planning.model.api.domain.usecase.deferral;

import java.time.Instant;

final class SaveDeferralReportLogger {

  private static final String MESSAGE_LOGGER = "Deleted %s sla Deferred report register of logisticCenter %s, with dateTo = %s to %s hs";
  private static final String MESSAGE_ERROR_DELETE = "Deleted view_date = %s. %s";
  
  private SaveDeferralReportLogger() {

  }

  static String generateLogMessage(
      final int deletedRegisters, final String logisticCenterId, final Instant dateToDelete, final Instant viewDate) {
    return String.format(MESSAGE_LOGGER, deletedRegisters, logisticCenterId, dateToDelete, viewDate);
  }

  static String generateLogError(final Instant date, final String errorMessage) {
    return String.format(MESSAGE_ERROR_DELETE, date, errorMessage);
  }


}
