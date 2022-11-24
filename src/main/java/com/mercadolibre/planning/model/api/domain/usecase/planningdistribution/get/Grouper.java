package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public enum Grouper {
  DATE_OUT,
  DATE_IN,
  PROCESS_PATH;

  private static final Map<String, Grouper> LOOKUP = Arrays.stream(values()).collect(
      toMap(Grouper::toString, Function.identity())
  );

  public static Optional<Grouper> of(final String value) {
    return Optional.ofNullable(LOOKUP.get(value.toUpperCase(Locale.US).replace('-', '_')));
  }

}
