package com.mercadolibre.planning.model.api.projection;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.util.TreeSet;
import lombok.Value;

@Value
public class Wave {
    ProcessPath processPath;
    int lowerBound;
    int upperBound;
    TreeSet<Instant> slaDates;
}
