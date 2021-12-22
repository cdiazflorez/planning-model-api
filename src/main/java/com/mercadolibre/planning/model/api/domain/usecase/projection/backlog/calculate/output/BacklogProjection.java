package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@AllArgsConstructor
public class BacklogProjection {

    private ProcessName processName;
    private List<BacklogProjectionOutputValue> values;
    private Source source;
}
