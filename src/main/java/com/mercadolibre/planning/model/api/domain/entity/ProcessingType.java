package com.mercadolibre.planning.model.api.domain.entity;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public enum ProcessingType {

  ACTIVE_WORKERS,
  ACTIVE_WORKERS_NS,
  EFFECTIVE_WORKERS,
  EFFECTIVE_WORKERS_NS,
  PERFORMED_PROCESSING,
  REMAINING_PROCESSING,
  MAX_CAPACITY,
  BACKLOG_LOWER_LIMIT,
  BACKLOG_UPPER_LIMIT,
  BACKLOG_LOWER_LIMIT_SHIPPING,
  BACKLOG_UPPER_LIMIT_SHIPPING,
  THROUGHPUT;

  private static final Map<String, ProcessingType> LOOKUP = Arrays.stream(values()).collect(
      toMap(ProcessingType::toString, Function.identity())
  );

  public static Optional<ProcessingType> of(final String value) {
    return Optional.ofNullable(LOOKUP.get(value.toUpperCase()));
  }

  @JsonValue
  public String toJson() {
    return toString().toLowerCase();
  }
}
