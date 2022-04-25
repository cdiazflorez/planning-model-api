package com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution;

import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Value;

/** Class containing the parameters for the units search request. */
@Value
@AllArgsConstructor
public class GetUnitsInput {

  ZonedDateTime dateFrom;

  ZonedDateTime dateTo;

  String wareHouseId;
}
