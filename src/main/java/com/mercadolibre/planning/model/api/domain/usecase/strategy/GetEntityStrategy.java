package com.mercadolibre.planning.model.api.domain.usecase.strategy;

import com.mercadolibre.planning.model.api.domain.usecase.GetEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Component
@AllArgsConstructor
public class GetEntityStrategy {

    private final GetHeadcountEntityUseCase getHeadcountEntityUseCase;

    private final GetProductivityEntityUseCase getProductivityEntityUseCase;

    public GetEntityUseCase getBy(final EntityType entityType) {
        return Stream.of(getHeadcountEntityUseCase, getProductivityEntityUseCase)
                .filter(useCase -> useCase.supportsEntityType(entityType))
                .findFirst()
                .get();
    }
}
