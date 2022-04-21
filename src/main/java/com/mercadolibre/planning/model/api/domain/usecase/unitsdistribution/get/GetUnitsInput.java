package com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.get;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class GetUnitsInput {

    ZonedDateTime dateFrom;
    ZonedDateTime dateTo;
    String wareHouseId;
}
