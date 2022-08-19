package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.web.controller.projection.request.CurrentBacklog;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BacklogProjectionInput {

    String logisticCenterId;

    List<EntityOutput> throughputs;

    List<GetPlanningDistributionOutput> planningUnits;

    ZonedDateTime dateFrom;

    ZonedDateTime dateTo;

    List<ProcessName> processNames;

    List<CurrentBacklog> currentBacklogs;

    Map<ZonedDateTime, Double> packingWallRatios;
}
