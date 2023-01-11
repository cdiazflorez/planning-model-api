package com.mercadolibre.planning.model.api.exception;

public class InvalidArgumentException extends RuntimeException {

    private static final String MESSAGE_TEMPLATE = "Invalid argument: %s";

    public InvalidArgumentException(final String message) {
        super(String.format(MESSAGE_TEMPLATE, message));
    }
}
