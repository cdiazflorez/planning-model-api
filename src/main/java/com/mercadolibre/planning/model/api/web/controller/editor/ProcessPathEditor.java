package com.mercadolibre.planning.model.api.web.controller.editor;

import static java.lang.String.format;
import static org.apache.logging.log4j.util.Strings.isBlank;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.beans.PropertyEditorSupport;
import java.util.Arrays;

public class ProcessPathEditor extends PropertyEditorSupport {

  private static final String BLANK_ERROR_MESSAGE = "Process Path value should not be blank";
  private static final String ERROR_MESSAGE_PATTER = "Value %s should be one of " + Arrays.toString(ProcessPath.values());

  @Override
  public void setAsText(final String text) {
    if (isBlank(text)) {
      throw new IllegalArgumentException(BLANK_ERROR_MESSAGE);
    }

    final ProcessPath processPath = ProcessPath.of(text)
        .orElseThrow(() -> new IllegalArgumentException(format(ERROR_MESSAGE_PATTER, text)));

    setValue(processPath);
  }
}
