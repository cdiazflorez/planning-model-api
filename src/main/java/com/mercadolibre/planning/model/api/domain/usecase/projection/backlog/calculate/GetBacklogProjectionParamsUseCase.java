package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;

public interface GetBacklogProjectionParamsUseCase {

    ProcessParams execute(final ProcessName processName,
                          final BacklogProjectionInput input);

    boolean supportsProcessName(ProcessName processNames);
}
