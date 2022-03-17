package com.mercadolibre.planning.model.api.exception;

public class EntityAlreadyExistsException extends RuntimeException {

    public static final String MESSAGE_PATTERN = "Entity %s with id %s already exists";

    private final String entityName;

    private final String entityId;

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
