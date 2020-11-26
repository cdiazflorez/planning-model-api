package com.mercadolibre.planning.model.api.domain.usecase.entities.input;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Value
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class GetProductivityInput extends GetEntityInput {

    private Set<Integer> abilityLevel;
}
