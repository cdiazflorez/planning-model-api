package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BacklogProjectionUseCaseFactory {

    private final Map<Workflow, GetBacklogProjectionUseCase> backlogProjections;

    @Autowired
    public BacklogProjectionUseCaseFactory(final Set<GetBacklogProjectionUseCase> useCases) {
        backlogProjections = useCases.stream()
                .collect(Collectors.toMap(
                        GetBacklogProjectionUseCase::getWorkflow,
                        Function.identity())
                );
    }

    public GetBacklogProjectionUseCase getUseCase(final Workflow workflow) {
        return backlogProjections.get(workflow);
    }
}
