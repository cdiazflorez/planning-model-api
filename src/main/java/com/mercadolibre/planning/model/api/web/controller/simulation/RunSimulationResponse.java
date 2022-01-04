package com.mercadolibre.planning.model.api.web.controller.simulation;

import com.mercadolibre.planning.model.api.domain.entity.sla.ProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Value
public class RunSimulationResponse {

    private ZonedDateTime date;

    private ZonedDateTime projectedEndDate;

    private ZonedDateTime simulatedEndDate;

    private int remainingQuantity;

    // TODO: remove this field if isn't needed for compatibility
    private ProcessingTime processingTime;

    // TODO: remove this field if isn't needed for compatibility
    private boolean isDeferred;

    public static List<RunSimulationResponse> fromProjectionOutputs(
            final List<CptProjectionOutput> simulationOutputs,
            final List<CptProjectionOutput> actualOutputs) {

        final Map<ZonedDateTime, CptProjectionOutput> actualOutputsByDate = actualOutputs
                .stream()
                .collect(Collectors.toMap(
                        CptProjectionOutput::getDate,
                        Function.identity(),
                        (p1, p2) -> p2
                ));

        if (simulationOutputs.size() == actualOutputs.size()) {
            return simulationOutputs.stream()
                    .map(s -> {
                        final ZonedDateTime simulatedEndDate = actualOutputsByDate.get(s.getDate())
                                .getProjectedEndDate();

                        return new RunSimulationResponse(
                                s.getDate(),
                                simulatedEndDate,
                                s.getProjectedEndDate(),
                                s.getRemainingQuantity(),
                                null,
                                false
                        );
                    })
                    .collect(Collectors.toList());
        } else {
            return emptyList();
        }
    }
}
