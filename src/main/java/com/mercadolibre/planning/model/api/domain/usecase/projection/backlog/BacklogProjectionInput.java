package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.entities.output.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.web.controller.request.projection.CurrentBacklog;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
@Builder
public class BacklogProjectionInput {

    private List<EntityOutput> throughputs;

    private List<GetPlanningDistributionOutput> planningUnits;

    private ZonedDateTime dateFrom;

    private ZonedDateTime dateTo;

    private List<ProcessName> processNames;

    private List<CurrentBacklog> currentBacklogs;
}
