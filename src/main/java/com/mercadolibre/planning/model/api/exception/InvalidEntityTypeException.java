package com.mercadolibre.planning.model.api.exception;

import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;

import java.util.Arrays;

import static java.lang.String.format;

public class InvalidEntityTypeException extends RuntimeException {

    public static final String MESSAGE_PATTERN = "Value %s is invalid, "
            + "instead it should be one of %s";

    private final String invalidEntityType;

    public InvalidEntityTypeException(final String entityType) {
        super();
        this.invalidEntityType = entityType;
    }

    @Override
    public String getMessage() {
        return format(MESSAGE_PATTERN, invalidEntityType, Arrays.toString(EntityType.values()));
    }
}
