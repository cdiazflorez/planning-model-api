package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.ZonedDateTime;
import lombok.Value;

@Value
public class PlanningDistributionOutput {
  GroupKey group;
  long total;

  @Value
  static class GroupKey {
    ProcessPath processPath;

    ZonedDateTime dateIn;

    ZonedDateTime dateOut;
  }
}
