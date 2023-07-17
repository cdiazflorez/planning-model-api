package com.mercadolibre.planning.model.api.adapter;

import com.mercadolibre.planning.model.api.domain.usecase.deferral.SaveDeferralReport;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

//Remove SuppressWarnings when the gateway is implemented
@SuppressWarnings("Coverage")
@Component
public class SaveDeferralReportAdapter implements SaveDeferralReport.DeferralGateway {
  @Override
  public int deleteDeferralReportBeforeDate(Instant dateTo) {
    return 0;
  }

  @Override
  public HttpStatus saveDeferralReport(String logisticCenterId, Instant date, List<SaveDeferralReport.SlaDeferred> slas) {
    return null;
  }
}
