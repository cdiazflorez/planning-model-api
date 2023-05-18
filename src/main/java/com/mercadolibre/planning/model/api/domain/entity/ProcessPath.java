package com.mercadolibre.planning.model.api.domain.entity;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

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
  PP_DEFAULT_MULTI,
  BULKY,
  SIOC,
  AMBIENT,
  REFRIGERATED;

  private static final Map<String, ProcessPath> LOOKUP = Arrays.stream(values()).collect(
      toMap(ProcessPath::toString, Function.identity())
  );

  public static Optional<ProcessPath> of(final String value) {
    return Optional.ofNullable(LOOKUP.get(value.toUpperCase(Locale.US).replace('-', '_')));
  }

  @JsonValue
  public String toJson() {
    return toString().toLowerCase(Locale.US);
  }

}
