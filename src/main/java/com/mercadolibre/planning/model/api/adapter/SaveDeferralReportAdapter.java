package com.mercadolibre.planning.model.api.adapter;

import com.mercadolibre.planning.model.api.client.db.repository.deferral.OutboundDeferralDataRepository;
import com.mercadolibre.planning.model.api.domain.entity.deferral.OutboundDeferralData;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.SaveDeferralReport.CptDeferred;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.SaveDeferralReport.DeferralReportGateway;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class SaveDeferralReportAdapter implements DeferralReportGateway {

  private final OutboundDeferralDataRepository outboundDeferralDataRepository;

  @Override
  public int deleteDeferralReportBeforeDate(Instant dateTo) {
    return 0;
  }

  @Override
  public void saveDeferralReport(
      final String logisticCenterId,
      final Instant deferralDate,
      final List<CptDeferred> cptDeferrals
  ) {

    final List<OutboundDeferralData> outboundDeferralDataList = cptDeferrals.stream()
        .map(cpt -> new OutboundDeferralData(
            logisticCenterId,
            deferralDate,
            cpt.getDate(),
            cpt.getStatus(),
            cpt.isUpdated()
        )).collect(Collectors.toList());

    outboundDeferralDataRepository.saveAll(outboundDeferralDataList);
  }
}
