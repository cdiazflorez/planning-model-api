package com.mercadolibre.planning.model.api.web.controller.entity;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.ORDERS;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum EntityType {
  HEADCOUNT(WORKERS, ProcessingType.EFFECTIVE_WORKERS),
  HEADCOUNT_SYSTEMIC(WORKERS, ProcessingType.EFFECTIVE_WORKERS),
  HEADCOUNT_NON_SYSTEMIC(WORKERS, ProcessingType.EFFECTIVE_WORKERS_NS),
  PRODUCTIVITY(UNITS_PER_HOUR, ProcessingType.PRODUCTIVITY),
  THROUGHPUT(UNITS_PER_HOUR, ProcessingType.THROUGHPUT),
  REMAINING_PROCESSING(UNITS, ProcessingType.REMAINING_PROCESSING),
  PERFORMED_PROCESSING(UNITS, ProcessingType.PERFORMED_PROCESSING),
  BACKLOG_LOWER_LIMIT(UNITS, ProcessingType.BACKLOG_LOWER_LIMIT),
  BACKLOG_UPPER_LIMIT(UNITS, ProcessingType.BACKLOG_UPPER_LIMIT),
  BACKLOG_LOWER_LIMIT_SHIPPING(ORDERS, ProcessingType.BACKLOG_LOWER_LIMIT_SHIPPING),
  BACKLOG_UPPER_LIMIT_SHIPPING(ORDERS, ProcessingType.BACKLOG_UPPER_LIMIT_SHIPPING),
  MAX_CAPACITY(UNITS_PER_HOUR, ProcessingType.MAX_CAPACITY);

  private static final Map<String, EntityType> LOOKUP = Arrays.stream(values()).collect(
      toMap(EntityType::toString, Function.identity())
  );
  private final MetricUnit metricUnit;
  private final ProcessingType processingType;

  public static Optional<EntityType> of(final String value) {
    return Optional.ofNullable(LOOKUP.get(value.toUpperCase(Locale.ROOT)));
  }

  @JsonValue
  public String toJson() {
    return toString().toLowerCase(Locale.ROOT);
  }
}
