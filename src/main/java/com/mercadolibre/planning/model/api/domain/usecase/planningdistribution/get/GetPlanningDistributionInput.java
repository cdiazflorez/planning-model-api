package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import static java.util.Collections.emptySet;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.Instant;
import java.util.Set;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@Builder
@EqualsAndHashCode
public class GetPlanningDistributionInput {
  private String warehouseId;

  private Workflow workflow;

  private Instant dateOutFrom;

  private Instant dateOutTo;

  private Instant dateInFrom;

  private Instant dateInTo;

  private Set<ProcessPath> processPaths;

  private boolean applyDeviation;

  private Instant viewDate;

  private boolean applyDeferrals;

  public Set<ProcessPath> getProcessPaths() {
    return processPaths == null ? emptySet() : processPaths;
  }
}
