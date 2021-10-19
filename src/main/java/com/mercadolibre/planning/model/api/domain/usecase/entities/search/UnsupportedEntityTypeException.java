package com.mercadolibre.planning.model.api.domain.usecase.entities.search;

import lombok.Value;

import static java.lang.String.format;

@Value
public class UnsupportedEntityTypeException extends RuntimeException {
    public static final String MESSAGE_PATTERN = "Unsupported Entity Type %s";

    private String entityType;

    @Override
    public String getMessage() {
        return format(MESSAGE_PATTERN, entityType);
    }
}
