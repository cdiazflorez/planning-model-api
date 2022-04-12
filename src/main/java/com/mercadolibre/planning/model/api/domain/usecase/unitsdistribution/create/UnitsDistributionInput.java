package com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.create;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
public class UnitsDistributionInput {

    List<UnitsInput> unitsDistribution;

}
