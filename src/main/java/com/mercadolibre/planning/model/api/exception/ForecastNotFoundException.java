package com.mercadolibre.planning.model.api.exception;

import java.util.Set;

public class ForecastNotFoundException extends RuntimeException {

    public static final String MESSAGE_PATTERN =
            "Forecast not present for workflow:%s, warehouse_id:%s and weeks:%s";

    private final String workflow;
    private final String warehouseId;
    private final Set<String> weeks;

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
