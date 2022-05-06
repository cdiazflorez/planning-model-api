package com.mercadolibre.planning.model.api.web.controller.projection.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.CurrentBacklogBySla;
import java.time.Instant;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Value;

/**
 * Backlog projection by area request body.
 */
@Value
public class BacklogProjectionByAreaRequest {

  @NotNull
  private Instant dateFrom;

  @NotNull
  private Instant dateTo;

  @NotNull
  private List<ProcessName> processName;

  private List<ThroughputDto> throughput;

  private List<GetPlanningDistributionOutput> planningUnits;

  private List<CurrentBacklogBySla> currentBacklog;

  private List<AreaShareAtSlaAndProcessDto> areaDistributions;

}
