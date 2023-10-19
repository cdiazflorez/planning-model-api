package com.mercadolibre.planning.model.api.exception;

public class DuplicateConfigurationException extends RuntimeException {

    public static final String MESSAGE_PATTERN = "Duplicate key configuration to save";

    private static final long serialVersionUID = 1L;

    public DuplicateConfigurationException() {
        super(MESSAGE_PATTERN);
    }

}
