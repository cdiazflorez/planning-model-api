package com.mercadolibre.planning.model.api.domain.usecase.projection;

import com.mercadolibre.planning.model.api.domain.usecase.entities.output.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetPlanningDistributionOutput;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
@Builder
public class ProjectionInput {

    private List<EntityOutput> throughput;

    private List<GetPlanningDistributionOutput> planningUnits;

    private ZonedDateTime dateFrom;

    private ZonedDateTime dateTo;

    private List<Backlog> backlog;
}
