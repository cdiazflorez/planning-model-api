package com.mercadolibre.planning.model.api.domain.usecase.deferral;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GetDeferred {

  private DeferredGateway deferredGateway;

  public List<DeferralStatus> getDeferred(final String logisticCenterID, final Workflow workflow, final Instant viewDate) {
    return deferredGateway.getDeferredWithStatus(logisticCenterID, workflow, viewDate);
  }

  /**
   * DeferredGateway retrieves the current deferral status of the deferred CPTs.
   */
  public interface DeferredGateway {
    List<DeferralStatus> getDeferredWithStatus(String logisticCenterId, Workflow workflow, Instant viewDate);
  }

  public record DeferralStatus(Instant date, DeferralType status) {
  }
}
