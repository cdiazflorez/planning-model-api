package com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Value
@Builder
public class SlaProjectionInput {

    private Workflow workflow;

    private String logisticCenterId;

    private Map<ZonedDateTime, Integer> capacity;

    private List<GetPlanningDistributionOutput> planningUnits;

    private ZonedDateTime dateFrom;

    private ZonedDateTime dateTo;

    private List<Backlog> backlog;

    private List<GetSlaByWarehouseOutput> slaByWarehouse;

    private ZonedDateTime currentDate;
}
