package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;

public interface BacklogProjectionUseCase
        extends UseCase<BacklogProjectionInput, ProcessParams> {

    boolean supportsProcessName(ProcessName processNames);
}
