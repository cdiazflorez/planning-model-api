package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.Instant;
import java.util.List;

/**
 * Deferral repository.
 * Obtain deferral information
 * */
public interface DeferralGateway {
    List<Instant> getDeferredCpts(String warehouseId, Workflow workflow, Instant viewDate);
}
