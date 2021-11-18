package com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.ProcessingTime;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class CptProjectionOutput {

    @JsonIgnore
    private CptCalculationOutput calculation;

    private ZonedDateTime etdCutoff;

    private ProcessingTime processingTime;

    private boolean isDeferred;

    // TODO Split class to CalculateProjectionUseCase and GetDeliveryPromiseUseCase
    public CptProjectionOutput(final ZonedDateTime date,
                               final ZonedDateTime projectedEndDate,
                               final int remainingQuantity,
                               final ZonedDateTime etdCutoff,
                               final ProcessingTime processingTime,
                               final boolean isDeferred) {

        calculation = new CptCalculationOutput(date, projectedEndDate, remainingQuantity);

        this.etdCutoff = etdCutoff;
        this.processingTime = processingTime;
        this.isDeferred = isDeferred;
    }

    public ZonedDateTime getDate() {
        return this.calculation.getDate();
    }

    public ZonedDateTime getProjectedEndDate() {
        return this.calculation.getProjectedEndDate();
    }

    public int getRemainingQuantity() {
        return this.calculation.getRemainingQuantity();
    }
}
