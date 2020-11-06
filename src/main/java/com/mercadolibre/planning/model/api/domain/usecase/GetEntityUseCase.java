package com.mercadolibre.planning.model.api.domain.usecase;

import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;

import java.util.List;

public interface GetEntityUseCase extends UseCase<GetEntityInput, List<EntityOutput>> {

    boolean supportsEntityType(EntityType entityType);
}