package com.mercadolibre.planning.model.api.projection;

import java.time.Instant;
import lombok.Value;

@Value
public class UnitsByDateOut {
    Instant dateOut;
    int units;
}
