package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.Map;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class ProcessParams {

    private ProcessName processName;
    private long currentBacklog;
    private Map<ZonedDateTime, Long> planningUnitsByDate;
    private Map<ZonedDateTime, Long> capacityByDate;
    private Map<ZonedDateTime, Long> previousBacklogsByDate;

}
