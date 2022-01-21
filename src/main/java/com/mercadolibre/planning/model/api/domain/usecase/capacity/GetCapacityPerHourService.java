package com.mercadolibre.planning.model.api.domain.usecase.capacity;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Service
@AllArgsConstructor
public class GetCapacityPerHourService {

    public List<CapacityOutput> execute(final Workflow workflow,
                                        final List<CapacityInput> capacityInputList) {

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
                                        getCapacityValue(workflow, capacityInputs))
                        ));
        return output;
    }

    private Integer getCapacityValue(final Workflow workflow, final List<CapacityInput> capacityInputs) {
        final Map<ProcessName, Long> capacityByProcess = capacityInputs.stream()
                .collect(groupingBy(
                        CapacityInput::getProcessName,
                        Collectors.mapping(
                                CapacityInput::getValue,
                                Collectors.reducing(0L, Long::sum)
                        )
                ));

        return workflow.getCapacityCalculator().apply(capacityByProcess).intValue();

    }

}
