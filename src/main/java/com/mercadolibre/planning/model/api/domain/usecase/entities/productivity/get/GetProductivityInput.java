package com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get;

import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
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
