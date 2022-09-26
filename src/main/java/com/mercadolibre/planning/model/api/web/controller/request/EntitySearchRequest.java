package com.mercadolibre.planning.model.api.web.controller.request;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.input.SearchEntitiesInput;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import com.mercadolibre.planning.model.api.web.controller.entity.request.EntityRequest;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EntitySearchRequest extends EntityRequest {

  @NotEmpty
  protected List<EntityType> entityTypes;

  private Map<EntityType, Map<String, List<String>>> entityFilters;

  public SearchEntitiesInput toSearchInput(final Workflow workflow) {
    return SearchEntitiesInput.builder()
        .warehouseId(warehouseId)
        .workflow(workflow)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .source(source)
        .processName(processName)
        .entityTypes(entityTypes)
        .entityFilters(entityFilters)
        .simulations(simulations)
        .viewDate(viewDate)
        .build();
  }
}
