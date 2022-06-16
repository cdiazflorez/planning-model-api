package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;

public interface GetBacklogProjectionParamsUseCase {

    ProcessParams execute(ProcessName processName, BacklogProjectionInput input);

    boolean supportsProcessName(ProcessName processNames);
}
