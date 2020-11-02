package com.mercadolibre.planning.model.api.web.controller.editor;

import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;

import java.beans.PropertyEditorSupport;
import java.util.Arrays;

import static java.lang.String.format;
import static org.apache.logging.log4j.util.Strings.isBlank;

public class ProcessingTypeEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(final String text) {
        if (isBlank(text)) {
            throw new IllegalArgumentException("Value should not be blank");
        }

        final ProcessingType processingType = ProcessingType.of(text).orElseThrow(() -> {
            final String message = format("Value %s should be one of %s",
                    text,
                    Arrays.toString(ProcessingType.values()));

            return new IllegalArgumentException(message);
        });

        setValue(processingType);
    }
}
