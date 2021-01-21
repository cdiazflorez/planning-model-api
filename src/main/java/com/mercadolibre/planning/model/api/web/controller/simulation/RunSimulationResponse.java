package com.mercadolibre.planning.model.api.web.controller.simulation;

import com.mercadolibre.planning.model.api.domain.usecase.projection.CptProjectionOutput;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

@AllArgsConstructor
@Data
public class RunSimulationResponse {

    private ZonedDateTime date;

    private ZonedDateTime projectedEndDate;

    private ZonedDateTime simulatedEndDate;

    private int remainingQuantity;

    public static List<RunSimulationResponse> fromProjectionOutputs(
            final List<CptProjectionOutput> simulationOutputs,
            final List<CptProjectionOutput> actualOutputs) {

        final List<RunSimulationResponse> runSimulationResponses = new ArrayList<>();

        final Map<ZonedDateTime, List<CptProjectionOutput>> actualOutputsByDate = actualOutputs
                .stream()
                .collect(groupingBy(CptProjectionOutput::getDate));

        if (simulationOutputs.size() == actualOutputs.size()) {
            simulationOutputs.forEach(s -> {
                runSimulationResponses.add(new RunSimulationResponse(
                        s.getDate(),
                        actualOutputsByDate.get(s.getDate()).get(0).getProjectedEndDate(),
                        s.getProjectedEndDate(),
                        s.getRemainingQuantity()
                ));
            });
        }

        return runSimulationResponses;
    }
}
