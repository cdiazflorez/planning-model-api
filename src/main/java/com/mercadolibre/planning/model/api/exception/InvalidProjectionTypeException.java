package com.mercadolibre.planning.model.api.exception;

import com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType;

import java.util.Arrays;

import static java.lang.String.format;

public class InvalidProjectionTypeException extends RuntimeException {

    public static final String MESSAGE_PATTERN = "Value %s is invalid, "
            + "instead it should be one of %s";

    private final String invalidProjectionType;

    public InvalidProjectionTypeException(final String invalidProjectionType) {
        super();
        this.invalidProjectionType = invalidProjectionType;
    }

    @Override
    public String getMessage() {
        return format(MESSAGE_PATTERN, invalidProjectionType,
                Arrays.toString(ProjectionType.values()));
    }
}
