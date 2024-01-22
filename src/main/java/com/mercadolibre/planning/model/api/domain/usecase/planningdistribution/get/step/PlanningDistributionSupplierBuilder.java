package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.step;

import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlannedUnitsService.Input;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlannedUnitsService.TaggedUnit;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PlanningDistributionSupplierBuilder {

  private PlanningDistributionGateway gateway;

  public Function<Input, Stream<TaggedUnit>> build() {
    return gateway::get;
  }

  /**
   * Gateway that returns the planned units as TaggedUnits based on the filters in the Input.
   *
   */
  public interface PlanningDistributionGateway {
    Stream<TaggedUnit> get(Input input);
  }

}
