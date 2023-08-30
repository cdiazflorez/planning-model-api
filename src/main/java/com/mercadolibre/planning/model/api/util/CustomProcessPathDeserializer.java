package com.mercadolibre.planning.model.api.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.io.IOException;

public class CustomProcessPathDeserializer extends JsonDeserializer<ProcessPath> {
  @Override
  public ProcessPath deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
    return ProcessPath.of(jsonParser.getText()).orElse(ProcessPath.UNKNOWN);
  }
}
