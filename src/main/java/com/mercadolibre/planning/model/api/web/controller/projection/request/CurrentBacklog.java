package com.mercadolibre.planning.model.api.web.controller.projection.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class CurrentBacklog {

    private ProcessName processName;
    private int quantity;
}
