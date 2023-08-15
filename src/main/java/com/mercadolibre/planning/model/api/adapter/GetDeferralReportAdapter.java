package com.mercadolibre.planning.model.api.adapter;

import com.mercadolibre.planning.model.api.client.db.repository.deferral.OutboundDeferralDataRepository;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.deferral.OutboundDeferralData;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.GetDeferralReport;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.DeferralGateway;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class GetDeferralReportAdapter implements GetDeferralReport.DeferralHistoryGateway, DeferralGateway {

    private static final Set<DeferralType> DEFERRAL_TYPES = Set.of(DeferralType.CAP_MAX, DeferralType.CASCADE);

    private OutboundDeferralDataRepository deferralRepository;

    @Override
    public List<GetDeferralReport.Deferral> getDeferralReport(final String logisticCenterId,
                                                              final Instant dateFrom,
                                                              final Instant dateTo) {

        final List<OutboundDeferralData> deferred = deferralRepository
                .findByLogisticCenterIdAndDateBetweenAndUpdatedIsTrue(logisticCenterId, dateFrom, dateTo);

        return deferred.stream()
                .map(deferralDto -> new GetDeferralReport.Deferral(
                        deferralDto.getDate(),
                        deferralDto.getCpt(),
                        deferralDto.getStatus()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<Instant> getDeferredCpts(final String warehouseId,
                                         final Workflow workflow,
                                         final Instant viewDate) {

        return deferralRepository.findDeferredCpts(warehouseId, viewDate, DEFERRAL_TYPES);

    }

}
