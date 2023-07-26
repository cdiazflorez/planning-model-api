package com.mercadolibre.planning.model.api.domain.usecase.deferral;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class GetDeferralReport {

    /**
     * This interface serves as a gateway to retrieve deferral history from the repository.
     */
    public interface DeferralHistoryGateway {
        /**
         * Gateway works in get deferral report from repository.
         *
         * @param logisticCenterId logistic center id
         * @param dateFrom        date from on filter
         * @param dateTo          date to on filter
         * @return List of deferral from report
         */
        List<Deferral> getDeferralReport(String logisticCenterId, Instant dateFrom, Instant dateTo);
    }

    /**
     * Represents a deferral entry in the deferral report.
     */
    @AllArgsConstructor
    @Value
    public static class Deferral {
        Instant cpt;
        DeferralType status;
        boolean updated;
    }
}
