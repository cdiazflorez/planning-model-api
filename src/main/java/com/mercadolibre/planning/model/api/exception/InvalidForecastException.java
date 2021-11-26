package com.mercadolibre.planning.model.api.exception;

public class InvalidForecastException extends RuntimeException {

    public static final String MESSAGE_PATTERN =
            "The currently loaded forecast is invalid or has missing values, "
                    + "warehouse_id:%s, workflow:%s";

    private final String workflow;
    private final String warehouseId;

    public InvalidForecastException(final String warehouseId,
                                    final String workflow) {
        super();
        this.warehouseId = warehouseId;
        this.workflow = workflow;
    }

    @Override
    public String getMessage() {
        return String.format(MESSAGE_PATTERN, warehouseId, workflow);
    }
}
