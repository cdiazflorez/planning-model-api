package com.mercadolibre.planning.model.api.exception;

import lombok.Builder;
import lombok.Value;

@Value
public class EntityNotFoundException extends RuntimeException {

    public static final String MESSAGE_PATTERN = "Entity %s with id %s was not found";

    private String entityName;

    private String entityId;

    @Builder
    public EntityNotFoundException(final String entityName, final String entityId) {
        super();
        this.entityName = entityName;
        this.entityId = entityId;
    }

    @Override
    public String getMessage() {
        return String.format(MESSAGE_PATTERN, entityName, entityId);
    }
}
