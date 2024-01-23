package com.mercadolibre.planning.model.api.exception;

import static java.lang.String.format;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.Serial;
import java.util.Map;

public class TagsParsingException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 42L;

  private static final String MESSAGE_PATTERN = "Error parsing tags: %s as JSON";

  private final Map<String, String> tags;

  public TagsParsingException(final Map<String, String> tags, final JsonProcessingException error) {
    super(error);
    this.tags = tags;
  }

  @Override
  public String getMessage() {
    return format(MESSAGE_PATTERN, tags);
  }
}
