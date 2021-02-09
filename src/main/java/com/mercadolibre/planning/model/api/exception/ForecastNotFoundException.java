package com.mercadolibre.planning.model.api.exception;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
public class ForecastNotFoundException extends RuntimeException {

    public static final String MESSAGE_PATTERN =
            "Forecast not present for workflow:%s, warehouse_id:%s and weeks:%s";

    private String workflow;
    private String warehouseId;
    private Set<String> weeks;

    @Builder
    public ForecastNotFoundException(final String workflow,
                                     final String warehouseId,
                                     final Set<String> weeks) {
        super();
        this.workflow = workflow;
        this.warehouseId = warehouseId;
        this.weeks = weeks;
    }

    @Override
    public String getMessage() {
        return String.format(MESSAGE_PATTERN, workflow, warehouseId, weeks);
    }
}
