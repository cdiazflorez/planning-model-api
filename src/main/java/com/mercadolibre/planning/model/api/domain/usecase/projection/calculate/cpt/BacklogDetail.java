package com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt;

import java.time.ZonedDateTime;
import java.util.Map;
import lombok.Value;

/**
 * Struct content total current backlog, total planed backlog and units by dates.
 */
@Value
public class BacklogDetail {
    int totalCurrentBacklog;

    int totalPlannedBacklog;

    Map<ZonedDateTime, Integer> totalBacklogByOperationHour;
}
