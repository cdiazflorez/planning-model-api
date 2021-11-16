package com.mercadolibre.planning.model.api.web.controller.simulation;

import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.ProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptCalculationOutput;
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

    private ProcessingTime processingTime;

    private boolean isDeferred;

    public static List<RunSimulationResponse> fromProjectionOutputs(
            final List<CptCalculationOutput> simulationOutputs,
            final List<CptCalculationOutput> actualOutputs) {

        final List<RunSimulationResponse> runSimulationResponses = new ArrayList<>();

        final Map<ZonedDateTime, List<CptCalculationOutput>> actualOutputsByDate = actualOutputs
                .stream()
                .collect(groupingBy(CptCalculationOutput::getDate));

        if (simulationOutputs.size() == actualOutputs.size()) {
            simulationOutputs.forEach(s -> {
                runSimulationResponses.add(new RunSimulationResponse(
                        s.getDate(),
                        actualOutputsByDate.get(s.getDate()).get(0).getProjectedEndDate(),
                        s.getProjectedEndDate(),
                        s.getRemainingQuantity(),
                        null, false));
            });
        }

        return runSimulationResponses;
    }
}
