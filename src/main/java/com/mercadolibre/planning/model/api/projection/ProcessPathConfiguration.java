package com.mercadolibre.planning.model.api.projection;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import lombok.Value;

@Value
public class ProcessPathConfiguration {
    ProcessPath processPath;
    int maxCycleTime;
    int normalCycleTime;
    int minCycleTime;
}
