package com.mercadolibre.planning.model.api.domain.usecase.entities.output;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;

@Value
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ProductivityOutput extends EntityOutput {

    private int abilityLevel;

    public boolean isMainProductivity() {
        return abilityLevel == 1;
    }

    public boolean isPolyvalentProductivity() {
        return abilityLevel == 2;
    }
}
