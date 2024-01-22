package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.step;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlannedUnitsService.Input;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlannedUnitsService.TaggedUnit;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.step.PlanningDistributionSupplierBuilder.PlanningDistributionGateway;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanningDistributionSupplierBuilderTest {

  private static final Input INPUT = new Input("ARTW01", FBM_WMS_OUTBOUND, Map.of("opt_1", "val_1", "opt_2", "val_2"));

  private static final List<TaggedUnit> MOCKED_UNITS = List.of(new TaggedUnit(0D, Map.of("mock", "true")));

  @InjectMocks
  private PlanningDistributionSupplierBuilder planningDistributionSupplierBuilder;

  @Mock
  private PlanningDistributionGateway planningDistributionGateway;

  @Test
  void test() {
    // GIVEN
    when(planningDistributionGateway.get(INPUT)).thenReturn(MOCKED_UNITS.stream());

    // WHEN
    final var supplier = planningDistributionSupplierBuilder.build();
    final var units = supplier.apply(INPUT);

    // THEN
    assertEquals(MOCKED_UNITS, units.toList());
  }
}
