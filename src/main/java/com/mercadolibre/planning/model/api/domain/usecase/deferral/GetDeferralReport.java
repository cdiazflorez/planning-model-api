package com.mercadolibre.planning.model.api.domain.usecase.deferral;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GetDeferralReport {

  private DeferralHistoryGateway deferralHistoryGateway;

  public Map<Instant, List<SlaStatus>> execute(final String logisticCenterId, final Instant dateFrom, final Instant dateTo) {
    final List<Deferral> deferrals = deferralHistoryGateway.getDeferralReport(logisticCenterId, dateFrom, dateTo);

    return deferrals.stream()
        .collect(
            Collectors.groupingBy(Deferral::getDate,
                Collectors.mapping(deferral -> new SlaStatus(deferral.getCpt(), deferral.getStatus()),
                    Collectors.toList()))
        );
  }

  /**
   * This interface serves as a gateway to retrieve deferral history from the repository.
   */
  public interface DeferralHistoryGateway {
    /**
     * Gateway works in get deferral report from repository.
     *
     * @param logisticCenterId logistic center id
     * @param dateFrom         date from on filter
     * @param dateTo           date to on filter
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
    Instant date;
    Instant cpt;
    DeferralType status;
  }

  /**
   * Represents sla with its deferral status.
   */
  @AllArgsConstructor
  @Value
  public static class SlaStatus {
    Instant date;
    DeferralType status;
  }
}
