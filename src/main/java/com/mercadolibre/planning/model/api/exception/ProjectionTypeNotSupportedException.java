package com.mercadolibre.planning.model.api.exception;

import com.mercadolibre.planning.model.api.web.controller.request.ProjectionType;
import lombok.Builder;
import lombok.Value;

import static java.lang.String.format;

@Value
public class ProjectionTypeNotSupportedException extends RuntimeException {

    public static final String MESSAGE_PATTERN = "Projection type %s is not supported";

    private ProjectionType projectionType;

    @Builder
    public ProjectionTypeNotSupportedException(final ProjectionType projectionType) {
        super();
        this.projectionType = projectionType;
    }

    @Override
    public String getMessage() {
        return format(MESSAGE_PATTERN, projectionType);
    }
}
