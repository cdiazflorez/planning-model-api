package com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get;

import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import java.time.Instant;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;

@Value
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class GetProductivityInput extends GetEntityInput {

  private Set<Integer> abilityLevel;

  public Instant viewDate() {
    return super.getViewDate() == null ? Instant.now() : super.getViewDate();
  }
}
