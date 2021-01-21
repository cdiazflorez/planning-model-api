package com.mercadolibre.planning.model.api.domain.usecase.entities;

import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;

import java.util.List;

public interface EntityUseCase<T extends GetEntityInput, R extends List<? extends EntityOutput>>
        extends UseCase<T, R> {

    boolean supportsEntityType(final EntityType entityType);

}
