package com.mercadolibre.planning.model.api.web.controller.editor;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;

import java.beans.PropertyEditorSupport;
import java.util.Arrays;

import static java.lang.String.format;
import static org.apache.logging.log4j.util.Strings.isBlank;

public class MetricUnitEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(final String text) {
        if (isBlank(text)) {
            throw new IllegalArgumentException("Value should not be blank");
        }

        final MetricUnit metricUnit = MetricUnit.of(text).orElseThrow(() -> {
            final String message = format("Value %s should be one of %s",
                    text,
                    Arrays.toString(MetricUnit.values()));

            return new IllegalArgumentException(message);
        });

        setValue(metricUnit);
    }
}
