package com.mercadolibre.planning.model.api.projection;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import lombok.Value;

/**
 * backlog grouped by process_path, by process_name and by date_out.
 */
@Value
public class UnitsByProcessPathAndProcess {
    ProcessPath processPath;
    ProcessName processName;
    Instant dateOut;
    int units;
}
