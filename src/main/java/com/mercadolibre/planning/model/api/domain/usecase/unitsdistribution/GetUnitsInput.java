package com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution;


import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@AllArgsConstructor
public class GetUnitsInput {

  ZonedDateTime dateFrom;

  ZonedDateTime dateTo;

  String wareHouseId;
}
