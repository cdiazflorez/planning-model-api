package com.mercadolibre.planning.model.api.web.controller.entity.request;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityInput;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProductivityRequest extends EntityRequest {

  private Set<Integer> abilityLevel;

  public GetProductivityInput toGetProductivityInput(final Workflow workflow) {
    return GetProductivityInput.builder()
        .warehouseId(warehouseId)
        .workflow(workflow)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .source(source)
        .processName(processName)
        .simulations(simulations)
        .abilityLevel(CollectionUtils.isEmpty(abilityLevel) ? Set.of(1) : abilityLevel)
        .viewDate(viewDate)
        .build();
  }
}
