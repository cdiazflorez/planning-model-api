package com.mercadolibre.planning.model.api.web.controller.editor;

import com.mercadolibre.planning.model.api.web.controller.request.EntityType;

import java.beans.PropertyEditorSupport;
import java.util.Arrays;

import static java.lang.String.format;
import static org.apache.logging.log4j.util.Strings.isBlank;

public class EntityTypeEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(final String text) {
        if (isBlank(text)) {
            throw new IllegalArgumentException("Value should not be blank");
        }

        final EntityType entityType = EntityType.of(text).orElseThrow(() -> {
            final String message = format("Value %s should be one of %s",
                    text,
                    Arrays.toString(EntityType.values()));

            return new IllegalArgumentException(message);
        });

        setValue(entityType);
    }
}
