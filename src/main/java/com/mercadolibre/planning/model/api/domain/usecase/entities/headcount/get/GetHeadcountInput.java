package com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get;

import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Value
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class GetHeadcountInput extends GetEntityInput {

    private Set<ProcessingType> processingType;

    public Instant viewDate() {
      return super.getViewDate() == null ? Instant.now() : super.getViewDate();
    }
}
