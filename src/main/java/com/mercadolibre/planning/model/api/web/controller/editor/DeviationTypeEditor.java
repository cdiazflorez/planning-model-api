package com.mercadolibre.planning.model.api.web.controller.editor;

import static java.lang.String.format;
import static org.apache.logging.log4j.util.Strings.isBlank;

import com.mercadolibre.planning.model.api.domain.entity.DeviationType;
import java.beans.PropertyEditorSupport;
import java.util.Arrays;

public class DeviationTypeEditor extends PropertyEditorSupport {
  @Override
  public void setAsText(final String text) {
    if (isBlank(text)) {
      throw new IllegalArgumentException("Value should not be blank");
    }

    final DeviationType deviationType = DeviationType.of(text).orElseThrow(() -> {
      final String message = format("Value %s should be one of %s",
          text,
          Arrays.toString(DeviationType.values()));
      return new IllegalArgumentException(message);

    });

    setValue(deviationType);
  }
}
