package com.mercadolibre.planning.model.api.projection;

import com.mercadolibre.planning.model.api.domain.entity.TriggerName;
import java.time.Instant;
import java.util.List;
import lombok.Value;

@Value
public class Suggestion {
    Instant date;
    List<BoundsByProcessPath> processPath;
    TriggerName reason;
    List<UnitsByDateOut> expectedQuantities;
}
