package com.mercadolibre.planning.model.api.projection.dto.response;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import lombok.Value;

@Value
public class ProcessPathResponse {
    ProcessPath name;
    int quantity;
}
