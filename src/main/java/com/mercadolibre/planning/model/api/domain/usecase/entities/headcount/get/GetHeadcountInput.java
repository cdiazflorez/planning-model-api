package com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get;

import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Value
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class GetHeadcountInput extends GetEntityInput {

    private Set<ProcessingType> processingType;
}
