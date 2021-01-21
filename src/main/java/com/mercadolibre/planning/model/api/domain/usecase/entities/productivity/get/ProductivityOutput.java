package com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get;

import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
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
