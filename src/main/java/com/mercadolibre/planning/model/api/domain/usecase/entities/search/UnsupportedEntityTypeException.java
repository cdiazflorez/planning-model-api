package com.mercadolibre.planning.model.api.domain.usecase.entities.search;

import lombok.RequiredArgsConstructor;

import static java.lang.String.format;

@RequiredArgsConstructor
public class UnsupportedEntityTypeException extends RuntimeException {
    public static final String MESSAGE_PATTERN = "Unsupported Entity Type %s";

    private final String entityType;

    @Override
    public String getMessage() {
        return format(MESSAGE_PATTERN, entityType);
    }
}
