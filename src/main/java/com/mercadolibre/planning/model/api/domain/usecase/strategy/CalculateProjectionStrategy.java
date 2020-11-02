package com.mercadolibre.planning.model.api.domain.usecase.strategy;

import com.mercadolibre.planning.model.api.domain.usecase.projection.CalculateProjectionUseCase;
import com.mercadolibre.planning.model.api.web.controller.request.ProjectionType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@AllArgsConstructor
public class CalculateProjectionStrategy {

    private final Set<CalculateProjectionUseCase> calculateProjectionUseCases;

    public Optional<CalculateProjectionUseCase> getBy(final ProjectionType projectionType) {
        return calculateProjectionUseCases.stream()
                .filter(useCase -> useCase.supportsProjectionType(projectionType))
                .findFirst();
    }
}
