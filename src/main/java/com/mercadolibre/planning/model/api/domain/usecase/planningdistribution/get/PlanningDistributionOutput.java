package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
public class PlanningDistributionOutput {
  GroupKey group;
  double total;

  @Value
  @Builder
  public static class GroupKey {
    ProcessPath processPath;

    Instant dateIn;

    Instant dateOut;
  }
}
