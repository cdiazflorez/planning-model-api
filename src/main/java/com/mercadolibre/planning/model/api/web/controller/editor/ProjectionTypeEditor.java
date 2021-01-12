package com.mercadolibre.planning.model.api.web.controller.editor;

import com.mercadolibre.planning.model.api.exception.InvalidProjectionTypeException;
import com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType;

import java.beans.PropertyEditorSupport;

import static org.apache.logging.log4j.util.Strings.isBlank;

public class ProjectionTypeEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(final String text) {
        if (isBlank(text)) {
            throw new IllegalArgumentException("Value should not be blank");
        }

        final ProjectionType projectionType = ProjectionType.of(text)
                .orElseThrow(() -> new InvalidProjectionTypeException(text));

        setValue(projectionType);
    }
}
