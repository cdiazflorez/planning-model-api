package com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Value
@Builder
public class CptProjectionInput {

    private Workflow workflow;

    private String logisticCenterId;

    private Map<ZonedDateTime, Integer> capacity;

    private List<GetPlanningDistributionOutput> planningUnits;

    private ZonedDateTime dateFrom;

    private ZonedDateTime dateTo;

    private List<Backlog> backlog;

    private ProjectionType projectionType;

    private ZonedDateTime currentDate;
}
