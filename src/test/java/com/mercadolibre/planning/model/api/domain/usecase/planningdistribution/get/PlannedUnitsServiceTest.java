package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlannedUnitsService.Input;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlannedUnitsService.TaggedUnit;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PlannedUnitsServiceTest {

  @ParameterizedTest
  @EnumSource(Workflow.class)
  void testGetPlannedUnits(final Workflow workflow) {
    // GIVEN
    final var input = new Input("ARTW01", workflow, emptyMap());

    // WHEN
    final var plannedUnits = new PlannedUnitsService().getPlannedUnits(input);

    // THEN
    assertEquals(new TaggedUnit(0D, Map.of("mock", "true")), plannedUnits.get(0));
  }
}
