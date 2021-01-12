package com.mercadolibre.planning.model.api.exception;

import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import lombok.Builder;
import lombok.Value;

import static java.lang.String.format;

@Value
public class EntityTypeNotSupportedException extends RuntimeException {

    public static final String MESSAGE_PATTERN = "Entity type %s is not supported";

    private EntityType entityType;

    @Builder
    public EntityTypeNotSupportedException(final EntityType entityType) {
        super();
        this.entityType = entityType;
    }

    @Override
    public String getMessage() {
        return format(MESSAGE_PATTERN, entityType);
    }
}
