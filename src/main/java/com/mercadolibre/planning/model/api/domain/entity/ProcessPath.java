package com.mercadolibre.planning.model.api.domain.entity;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.mercadolibre.planning.model.api.util.CustomProcessPathDeserializer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@JsonDeserialize(using = CustomProcessPathDeserializer.class)
public enum ProcessPath {
  GLOBAL,
  TOT_MONO,
  TOT_SINGLE_SKU,
  TOT_MULTI_BATCH,
  TOT_MULTI_ORDER,
  PP_DEFAULT_MONO,
  NON_TOT_MONO,
  NON_TOT_MULTI_ORDER,
  NON_TOT_MULTI_BATCH,
  NON_TOT_SINGLE_SKU,
  PP_DEFAULT_MULTI,
  BULKY,
  SIOC,
  AMBIENT,
  REFRIGERATED,
  UNKNOWN;

  private static final Map<String, ProcessPath> LOOKUP = Arrays.stream(values()).collect(
      toMap(ProcessPath::toString, Function.identity())
  );

  public static Optional<ProcessPath> of(final String value) {

    return Optional.of(LOOKUP.getOrDefault(value.toUpperCase(Locale.US).replace('-', '_'), UNKNOWN));
  }

  @JsonValue
  public String toJson() {
    return toString().toLowerCase(Locale.US);
  }

}
