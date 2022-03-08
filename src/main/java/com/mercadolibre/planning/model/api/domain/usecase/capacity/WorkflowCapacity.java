package com.mercadolibre.planning.model.api.domain.usecase.capacity;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PUT_AWAY;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.util.Map;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Encapsulates the algorithms to calculate the available throughput by workflow. */
@AllArgsConstructor
public enum WorkflowCapacity {
  FBM_WMS_INBOUND(WorkflowCapacity::calculateInboundCapacity),
  FBM_WMS_OUTBOUND(WorkflowCapacity::calculateOutboundCapacity);

  @Getter
  private final Function<Map<ProcessName, Long>, Long> capacityCalculator;

  private static long calculateInboundCapacity(final Map<ProcessName, Long> capacities) {
    return capacities.getOrDefault(PUT_AWAY, 0L);
  }

  private static long calculateOutboundCapacity(final Map<ProcessName, Long> capacities) {
    return capacities.getOrDefault(PACKING, 0L)
        + capacities.getOrDefault(PACKING_WALL, 0L);
  }

  private static WorkflowCapacity from(final Workflow globalWorkflow) {
    return WorkflowCapacity.valueOf(globalWorkflow.name());
  }

  /**
   * Calculates the total available throughout for a workflow.
   *
   * <p>
   * The selection of the algorithm is based on the workflow.
   * </p>
   *
   * @param  workflow   the target workflow.
   * @param  capacities the throughput for the processes of the workflow (or at least those processes required by the algorithms)
   *                    for a determined instant.
   * @return            the total capacity for the process.
   */
  public static Integer getCapacity(final Workflow workflow, final Map<ProcessName, Long> capacities) {
    return from(workflow).capacityCalculator.apply(capacities).intValue();
  }

}
