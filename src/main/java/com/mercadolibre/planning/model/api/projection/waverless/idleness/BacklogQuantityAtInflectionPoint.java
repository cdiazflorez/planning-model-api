package com.mercadolibre.planning.model.api.projection.waverless.idleness;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import java.time.Instant;
import lombok.Value;

/**
 * @author (Andrés M. Barragán) anbarragan
 */
@Value
public class BacklogQuantityAtInflectionPoint {

  Instant inflectionPoint;

  ProcessName processName;

  Long quantity;

}
