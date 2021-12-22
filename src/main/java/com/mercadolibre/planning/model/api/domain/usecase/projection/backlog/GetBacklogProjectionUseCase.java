package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BacklogProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.BacklogProjection;

import java.util.List;

public interface GetBacklogProjectionUseCase {

    List<BacklogProjection> execute(final BacklogProjectionInput input);

    Workflow getWorkflow();
}
