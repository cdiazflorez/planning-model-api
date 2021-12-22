package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.web.controller.projection.request.CurrentBacklog;

import java.util.List;

public interface GetBacklogProjectionParamsUseCase {

    ProcessParams execute(final ProcessName processName,
                          final BacklogProjectionInput input);

    boolean supportsProcessName(ProcessName processNames);
}
