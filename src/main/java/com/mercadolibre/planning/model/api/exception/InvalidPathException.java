package com.mercadolibre.planning.model.api.exception;

public class InvalidPathException extends RuntimeException {

    public static final String MESSAGE = "path from servlet request is null";

    private static final long serialVersionUID = -3435018287308090586L;

    public InvalidPathException() {
        super(MESSAGE);
    }

}
