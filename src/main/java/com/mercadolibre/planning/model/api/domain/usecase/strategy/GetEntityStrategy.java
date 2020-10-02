package com.mercadolibre.planning.model.api.domain.usecase.strategy;

import com.mercadolibre.planning.model.api.domain.usecase.GetEntityUseCase;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@AllArgsConstructor
public class GetEntityStrategy {

    private final Set<GetEntityUseCase> getEntityUseCases;

    public Optional<GetEntityUseCase> getBy(final EntityType entityType) {
        return getEntityUseCases.stream()
                .filter(useCase -> useCase.supportsEntityType(entityType))
                .findFirst();
    }
}
