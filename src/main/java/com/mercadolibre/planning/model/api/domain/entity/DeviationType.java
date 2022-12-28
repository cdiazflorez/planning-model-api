package com.mercadolibre.planning.model.api.domain.entity;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public enum DeviationType {
  UNITS,
  MINUTES;

  private static final Map<String, DeviationType> LOOKUP = Arrays.stream(values()).collect(
      toMap(DeviationType::toString, Function.identity())
  );

  /**
   * Deserialization of a String that represents a deviation type.
   *
   * @param value the selected deviation type.
   * @return an Optional with the desired  deviation type if the String matches a valid representation.
   */
  @JsonCreator
  public static Optional<DeviationType> of(final String value) {
    return Optional.ofNullable(LOOKUP.get(value.toUpperCase(Locale.US)));
  }

  public String getName() {
    return name().toLowerCase(Locale.ROOT);
  }
}
