package com.mercadolibre.planning.model.api.projection.dto.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.util.Set;
import lombok.Value;

@Value
public class ProcessPathRequest {
    ProcessPath name;
    Set<Quantity> quantity;
    int total;
}
