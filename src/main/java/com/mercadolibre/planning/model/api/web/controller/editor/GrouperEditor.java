package com.mercadolibre.planning.model.api.web.controller.editor;

import static java.lang.String.format;
import static org.apache.logging.log4j.util.Strings.isBlank;

import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.Grouper;
import java.beans.PropertyEditorSupport;
import java.util.Arrays;

public class GrouperEditor extends PropertyEditorSupport {

  private static final String BLANK_ERROR_MESSAGE = "Grouper value should not be blank";
  private static final String ERROR_MESSAGE_PATTER = "Value %s should be one of " + Arrays.toString(Grouper.values());

  @Override
  public void setAsText(final String text) {
    if (isBlank(text)) {
      throw new IllegalArgumentException(BLANK_ERROR_MESSAGE);
    }

    final Grouper grouper = Grouper.of(text)
        .orElseThrow(() -> new IllegalArgumentException(format(ERROR_MESSAGE_PATTER, text)));

    setValue(grouper);
  }
}
