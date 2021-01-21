package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.BacklogProjectionUseCase;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@AllArgsConstructor
public class BacklogProjectionStrategy {

    private final Set<BacklogProjectionUseCase> backlogProjectionUseCases;

    public Optional<BacklogProjectionUseCase> getBy(final ProcessName processName) {
        return backlogProjectionUseCases.stream()
                .filter(useCase -> useCase.supportsProcessName(processName))
                .findFirst();
    }
}
