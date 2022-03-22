package com.mercadolibre.planning.model.api.exception;

import com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType;

import static java.lang.String.format;

public class ProjectionTypeNotSupportedException extends RuntimeException {

    public static final String MESSAGE_PATTERN = "Projection type %s is not supported";

    private final ProjectionType projectionType;

    public ProjectionTypeNotSupportedException(final ProjectionType projectionType) {
        super();
        this.projectionType = projectionType;
    }

    @Override
    public String getMessage() {
        return format(MESSAGE_PATTERN, projectionType);
    }
}
