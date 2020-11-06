package com.mercadolibre.planning.model.api.exception;

import lombok.Builder;
import lombok.Value;

@Value
public class EntityAlreadyExistsException extends RuntimeException {

    public static final String MESSAGE_PATTERN = "Entity %s with id %s already exists";

    private String entityName;

    private String entityId;

    @Builder
    public EntityAlreadyExistsException(final String entityName, final String entityId) {
        super();
        this.entityName = entityName;
        this.entityId = entityId;
    }

    @Override
    public String getMessage() {
        return String.format(MESSAGE_PATTERN, entityName, entityId);
    }
}
