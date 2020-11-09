package com.mercadolibre.planning.model.api.web.controller.simulation;

import com.mercadolibre.planning.model.api.domain.usecase.projection.ProjectionOutput;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.ZonedDateTime;

@AllArgsConstructor
@Data
public class SaveSimulationResponse {

    private ZonedDateTime date;

    private ZonedDateTime projectedEndDate;

    private int remainingQuantity;

    public static SaveSimulationResponse fromProjectionOutput(final ProjectionOutput output) {
        return new SaveSimulationResponse(
                output.getDate(), output.getProjectedEndDate(), output.getRemainingQuantity());
    }
}
