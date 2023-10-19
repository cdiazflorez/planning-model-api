package com.mercadolibre.planning.model.api.exception;

public class DuplicateConfigurationException extends RuntimeException {

    public static final String MESSAGE_PATTERN = "Duplicate key configuration to save";

    public DuplicateConfigurationException() {
        super(MESSAGE_PATTERN);
    }

}
