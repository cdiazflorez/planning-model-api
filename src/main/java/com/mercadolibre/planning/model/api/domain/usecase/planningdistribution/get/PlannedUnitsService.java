package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.util.List;
import java.util.Map;

/**
 * Planned units service obtains the planned units as TaggedUnits by executing a pipeline of steps
 * for each worklfow.
 */
public class PlannedUnitsService {

  public List<TaggedUnit> getPlannedUnits(final Input input) {
    // TOOD: replace with real implementation
    return List.of(
        new TaggedUnit(0D, Map.of("mock", "true"))
    );
  }

  public record Input(String networkNode, Workflow workflow, Map<String, String> options) {
  }

  public record TaggedUnit(double quantity, Map<String, String> tags) {
  }
}
