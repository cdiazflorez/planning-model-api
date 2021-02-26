package com.mercadolibre.planning.model.api.web.controller.editor;

import java.beans.PropertyEditorSupport;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import static java.lang.String.format;
import static org.apache.logging.log4j.util.Strings.isBlank;

public class ZonedDateTimeEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(final String text) {
        if (isBlank(text)) {
            throw new IllegalArgumentException("Value should not be blank");
        }

        try {
            setValue(ZonedDateTime.parse(text));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(format("Value %s should be ZonedDateTime", text), e);
        }
    }
}
