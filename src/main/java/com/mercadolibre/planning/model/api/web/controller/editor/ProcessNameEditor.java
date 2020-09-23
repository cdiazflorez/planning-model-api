package com.mercadolibre.planning.model.api.web.controller.editor;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;

import java.beans.PropertyEditorSupport;
import java.util.Arrays;

import static java.lang.String.format;
import static org.apache.logging.log4j.util.Strings.isBlank;

public class ProcessNameEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(final String text) {
        if (isBlank(text)) {
            throw new IllegalArgumentException("Value should not be blank");
        }

        final ProcessName processName = ProcessName.of(text).orElseThrow(() -> {
            final String message = format("Value %s should be one of %s",
                    text,
                    Arrays.toString(ProcessName.values()));

            return new IllegalArgumentException(message);
        });

        setValue(processName);
    }
}
