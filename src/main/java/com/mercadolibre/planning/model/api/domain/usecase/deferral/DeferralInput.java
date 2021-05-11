package com.mercadolibre.planning.model.api.domain.usecase.deferral;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import lombok.Value;

@Value
public class DeferralInput {

    private final String logisticCenterId;

    private final Workflow workflow;
}
