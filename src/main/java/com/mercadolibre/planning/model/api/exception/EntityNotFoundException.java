package com.mercadolibre.planning.model.api.exception;

public class EntityNotFoundException extends RuntimeException {

    public static final String MESSAGE_PATTERN = "Entity %s with id %s was not found";

    private final String entityName;

    private final String entityId;

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
