package com.mercadolibre.planning.model.api.projection;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import lombok.Value;

@Value
public class BoundsByProcessPath {
    ProcessPath processPath;
    int lowerBound;
    int upperBound;
}
