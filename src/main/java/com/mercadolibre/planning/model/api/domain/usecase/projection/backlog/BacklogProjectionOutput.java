package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.web.controller.request.Source;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class BacklogProjectionOutput {

    private ProcessName processName;
    private List<BacklogProjectionOutputValue> values;
    private Source source;
}
