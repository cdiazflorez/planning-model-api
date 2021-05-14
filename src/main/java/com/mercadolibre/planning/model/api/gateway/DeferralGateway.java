package com.mercadolibre.planning.model.api.gateway;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralDto;

public interface DeferralGateway {

    DeferralDto getDeferralProjection(final String warehouseId, final Workflow workflow);
}
