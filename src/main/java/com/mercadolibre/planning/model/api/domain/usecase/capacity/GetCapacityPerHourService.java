package com.mercadolibre.planning.model.api.domain.usecase.capacity;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static java.lang.Math.min;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.newrelic.api.agent.Trace;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/** Implements the behaviour to calculate the total capacity for a Workflow in a Logistic Center. */
@Service
@AllArgsConstructor
public class GetCapacityPerHourService {

  /**
   * Calculates the total capacity or throughput for the Workflow.
   *
   * <p>
   * The calculation depends on the selected Workflow.
   * </p>
   *
   * @param  workflow          workflow over which to calculate the capacity.
   * @param  capacityInputList available throughput per date and process.
   * @return                   one record for each instant in time with the available capacity at that instant.
   */
  @Trace
  public List<CapacityOutput> execute(final Workflow workflow,
                                      final List<CapacityInput> capacityInputList) {

    final boolean hasPickingCapacity = workflow == Workflow.FBM_WMS_OUTBOUND
        && capacityInputList.stream()
        .anyMatch(ci -> ci.getProcessName().equals(PICKING));

    final List<CapacityOutput> output = new ArrayList<>();
    capacityInputList
        .stream()
        .collect(
            groupingBy(
                entityOutput -> entityOutput.getDate()
                    .withFixedOffsetZone(),
                TreeMap::new,
                toList()
            )
        ).forEach((entityDate, capacityInputs) ->
            output.add(
                new CapacityOutput(entityDate, UNITS_PER_HOUR,
                    getCapacityValue(workflow, capacityInputs, hasPickingCapacity))
            ));
    return output;
  }

  private Integer getCapacityValue(final Workflow workflow,
                                   final List<CapacityInput> capacityInputs,
                                   final boolean hasPickingCapacity) {

    final Map<ProcessName, Long> capacityByProcess = capacityInputs.stream()
        .collect(groupingBy(
            CapacityInput::getProcessName,
            Collectors.mapping(
                CapacityInput::getValue,
                Collectors.reducing(0L, Long::sum)
            )
        ));

    // TODO: remove after FT no longer applies
    if (hasPickingCapacity) {
      return capacityByProcess.getOrDefault(PICKING, 0L).intValue();
    }

    return WorkflowCapacity.getCapacity(workflow, capacityByProcess);
  }

}
