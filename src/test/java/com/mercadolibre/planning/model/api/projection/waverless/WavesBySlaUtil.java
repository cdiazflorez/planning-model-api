package com.mercadolibre.planning.model.api.projection.waverless;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class WavesBySlaUtil {

  public static final Instant FIRST_INFLECTION_POINT = Instant.parse("2023-03-06T00:00:00Z");

  public static final Instant LAST_INFLECTION_POINT = Instant.parse("2023-03-06T06:00:00Z");

  public static final Instant SLA_1 = Instant.parse("2023-03-06T03:00:00Z");

  public static final Instant SLA_2 = Instant.parse("2023-03-06T04:00:00Z");

  public static final Instant SLA_3 = Instant.parse("2023-03-06T05:00:00Z");

  public static final Map<Instant, Integer> TPH = Map.of(
      Instant.parse("2023-03-06T00:00:00Z"), 60,
      Instant.parse("2023-03-06T01:00:00Z"), 60,
      Instant.parse("2023-03-06T02:00:00Z"), 60,
      Instant.parse("2023-03-06T03:00:00Z"), 60,
      Instant.parse("2023-03-06T04:00:00Z"), 60,
      Instant.parse("2023-03-06T05:00:00Z"), 60
  );

  static final List<Instant> INFLECTION_POINTS = Stream.iterate(FIRST_INFLECTION_POINT, date -> date.plus(5, ChronoUnit.MINUTES))
      .limit((ChronoUnit.MINUTES.between(FIRST_INFLECTION_POINT, LAST_INFLECTION_POINT) / 5) + 1)
      .collect(Collectors.toList());

  static final Map<ProcessPath, Integer> MIN_CYCLE_TIMES = Map.of(TOT_MONO, 60, TOT_MULTI_BATCH, 60);

  static final Map<ProcessPath, Map<ProcessName, Map<Instant, Integer>>> THROUGHPUT = Map.of(
      TOT_MONO, Map.of(
          ProcessName.PICKING, TPH
      )
  );

  static final Map<ProcessPath, Map<Instant, Integer>> PICKING_THROUGHPUT = Map.of(
      TOT_MONO, TPH
  );

  private WavesBySlaUtil() {
  }

  static Instant inflectionPoint(final int id) {
    return FIRST_INFLECTION_POINT.plus(id * 5L, ChronoUnit.MINUTES);
  }
}
