package com.mercadolibre.planning.model.api.domain.usecase;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.BacklogProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.ProcessParams;

public interface BacklogProjectionUseCase
        extends UseCase<BacklogProjectionInput, ProcessParams> {

    boolean supportsProcessName(ProcessName processNames);
}
