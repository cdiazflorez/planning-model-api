package com.mercadolibre.planning.model.api.usecase.capacity;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.WorkflowCapacity;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests {@link WorkflowCapacity}. */
public class WorkflowCapacityTest {

  private static Stream<Arguments> parameters() {
    return Stream.of(
        arguments(FBM_WMS_INBOUND, Map.of(), 0),
        arguments(FBM_WMS_INBOUND, Map.of(PUT_AWAY, 10L), 10),
        arguments(FBM_WMS_OUTBOUND, Map.of(), 0),
        arguments(FBM_WMS_OUTBOUND, Map.of(PACKING, 10L), 10),
        arguments(FBM_WMS_OUTBOUND, Map.of(PACKING_WALL, 10L), 10),
        arguments(
            FBM_WMS_OUTBOUND,
            Map.of(
                PACKING, 6L,
                PACKING_WALL, 5L),
            11)
    );
  }

  /**
   * Tests capacity calculation algorithm.
   *
   * @param  workflow   target Workflow.
   * @param  capacities input capacities.
   * @param  expected   expected output.
   */
  @ParameterizedTest
  @MethodSource("parameters")
  public void testCapacity(final Workflow workflow, final Map<ProcessName, Long> capacities, final int expected) {
    final var result = WorkflowCapacity.getCapacity(workflow, capacities);
    assertEquals(expected, result);
  }

}
