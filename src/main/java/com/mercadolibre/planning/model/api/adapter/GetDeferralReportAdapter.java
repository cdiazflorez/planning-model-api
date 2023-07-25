package com.mercadolibre.planning.model.api.adapter;

import com.mercadolibre.planning.model.api.client.db.repository.deferral.OutboundDeferralDataRepository;
import com.mercadolibre.planning.model.api.domain.entity.deferral.OutboundDeferralData;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.GetDeferralReport;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class GetDeferralReportAdapter implements GetDeferralReport.DeferralHistoryGateway {

    private OutboundDeferralDataRepository deferralRepository;

    @Override
    public List<GetDeferralReport.Deferral> getDeferralReport(final String logisticCenterId,
                                                              final Instant dateFrom,
                                                              final Instant dateTo) {

        final List<OutboundDeferralData> deferred = deferralRepository
                .findByLogisticCenterIdAndDateBetweenAndUpdatedIsTrue(logisticCenterId, dateFrom, dateTo);

        return deferred.stream()
                .map(deferralDto -> new GetDeferralReport.Deferral(
                        deferralDto.getCpt(),
                        deferralDto.getStatus(),
                        deferralDto.getUpdated()
                ))
                .collect(Collectors.toList());
    }
}
