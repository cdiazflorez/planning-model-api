package com.mercadolibre.planning.model.api.domain.usecase;

import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetEntityOutput;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;

import java.util.List;

public interface GetEntityUseCase extends UseCase<GetEntityInput, List<GetEntityOutput>> {

    boolean supportsEntityType(EntityType entityType);
}
