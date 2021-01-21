package com.mercadolibre.planning.model.api.domain.usecase.entities.input;

import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SearchEntitiesInput extends GetEntityInput {

    private List<EntityType> entityTypes;

    private Map<EntityType, Map<String, List<String>>> entityFilters;

}
