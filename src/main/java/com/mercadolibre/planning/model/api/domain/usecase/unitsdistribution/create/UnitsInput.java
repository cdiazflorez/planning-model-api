package com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.create;

import lombok.Builder;
import lombok.Value;
import java.time.ZonedDateTime;

@Builder
@Value
public class UnitsInput {

    String logisticCenterId;

    ZonedDateTime date;

    String processName;

    String area;

    Double quantity;

    String quantityMetricUnit;
}
