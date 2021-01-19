package com.mercadolibre.planning.model.api.domain.usecase.capacity;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static java.lang.Math.min;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Service
@AllArgsConstructor
public class GetCapacityPerHourUseCase
        implements UseCase<List<CapacityInput>, List<CapacityOutput>> {

    @Override
    public List<CapacityOutput> execute(final List<CapacityInput> capacityInputList) {
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
                                            getCapacityValue(capacityInputs)))
            );
        return output;
    }

    private Integer getCapacityValue(final List<CapacityInput> capacityInputs) {
        final Workflow currentWorkflow = capacityInputs.stream()
                .findFirst()
                .map(CapacityInput::getWorkflow)
                .orElse(null);

        Integer quantity = null;
        if (currentWorkflow == Workflow.FBM_WMS_OUTBOUND) {
            quantity = (int) min(capacityInputs.stream()
                            .filter(entityOutput -> entityOutput.getProcessName() == PICKING)
                            .mapToLong(CapacityInput::getValue)
                            .sum(),
                    capacityInputs.stream()
                            .filter(entityOutput -> entityOutput.getProcessName() == PACKING
                                    || entityOutput.getProcessName() == PACKING_WALL)
                            .mapToLong(CapacityInput::getValue)
                            .sum());
        }
        return quantity;

    }



}
