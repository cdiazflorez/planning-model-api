package com.mercadolibre.planning.model.api.web.controller.editor;

import com.mercadolibre.planning.model.api.exception.InvalidEntityTypeException;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;

import java.beans.PropertyEditorSupport;

import static org.apache.logging.log4j.util.Strings.isBlank;

public class EntityTypeEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(final String text) {
        if (isBlank(text)) {
            throw new IllegalArgumentException("Value should not be blank");
        }

        final EntityType entityType = EntityType.of(text)
                .orElseThrow(() -> new InvalidEntityTypeException(text));

        setValue(entityType);
    }
}
