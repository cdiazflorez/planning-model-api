package com.mercadolibre.planning.model.api.domain.usecase.entities;

import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@AllArgsConstructor
public class GetEntitiesStrategy {

    private final Set<EntityUseCase> entityUseCases;

    public Optional<EntityUseCase> getBy(final EntityType entityType) {
        return entityUseCases.stream()
                .filter(useCase -> useCase.supportsEntityType(entityType))
                .findFirst();
    }
}
