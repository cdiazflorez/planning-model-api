package com.mercadolibre.planning.model.api.projection.dto.request;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BacklogProjectionTest {

  private static final Instant DATE_IN_1 = Instant.parse("2023-05-21T10:00:00Z");

  private static final Instant DATE_IN_2 = Instant.parse("2023-05-21T11:00:00Z");

  private static final Instant DATE_IN_3 = Instant.parse("2023-05-21T12:00:00Z");

  private static final Instant DATE_OUT_1 = Instant.parse("2023-05-21T16:00:00Z");

  private static final Instant DATE_FROM = Instant.parse("2023-05-21T10:00:00Z");

  private static final Instant DATE_TO = Instant.parse("2023-05-21T12:00:00Z");

  @Test
  void mapForecast() {
    final PlannedUnit plannedUnits = new PlannedUnit(
        List.of(
            new ProcessPathRequest(
                TOT_MONO,
                Set.of(
                    new Quantity(
                        DATE_IN_1,
                        DATE_OUT_1,
                        100
                    ),
                    new Quantity(
                        DATE_IN_2,
                        DATE_OUT_1,
                        200
                    ),
                    new Quantity(
                        DATE_IN_3,
                        DATE_OUT_1,
                        300
                    )
                ))
        )
    );

    final BacklogProjection backlogProjection = new BacklogProjection(
        null,
        plannedUnits,
        null,
        DATE_FROM,
        DATE_TO,
        Workflow.FBM_WMS_OUTBOUND
    );

    final var expected = Map.of(
        DATE_FROM, Map.of(
            TOT_MONO, Map.of(
                DATE_OUT_1, 100L
            )
        ),
        DATE_FROM.plus(1, ChronoUnit.HOURS), Map.of(
            TOT_MONO, Map.of(
                DATE_OUT_1, 200L
            )
        ),
        DATE_FROM.plus(2, ChronoUnit.HOURS), Map.of(
            TOT_MONO, Map.of(
                DATE_OUT_1, 300L
            )
        )
    );

    final var response = backlogProjection.mapForecast();
    assertNotNull(response);
    assertEquals(expected, response);
  }

  @Test
  void mapBacklog() {
    // GIVEN
    final Backlog backlog = new Backlog(List.of(
        new Process(WAVING, List.of(
            new Process.ProcessPathDetail(TOT_MONO, List.of(
                new Process.QuantityByDate(DATE_OUT_1, 200)
            ))
        )),
        new Process(PACKING, List.of(
            new Process.ProcessPathDetail(
                TOT_MONO, List.of(
                new Process.QuantityByDate(DATE_OUT_1, 200)
            ))
            ))
    ));

    final var expected = Map.of(
        PACKING, Map.of(
            TOT_MONO, Map.of(
                DATE_OUT_1, 200L
            )
        ),
        WAVING, Map.of(
            TOT_MONO, Map.of(
                DATE_OUT_1, 200L
            )
        )
    );

    final BacklogProjection backlogProjection = new BacklogProjection(
        backlog,
        null,
        null,
        DATE_FROM,
        DATE_TO,
        Workflow.FBM_WMS_OUTBOUND
    );

    final var response = backlogProjection.mapBacklogs();
    assertNotNull(response);
    assertEquals(expected.get(PACKING).get(TOT_MONO).get(DATE_OUT_1), response.get(PACKING).get(TOT_MONO).get(DATE_OUT_1));
    assertEquals(expected.get(WAVING).get(TOT_MONO).get(DATE_OUT_1), response.get(WAVING).get(TOT_MONO).get(DATE_OUT_1));
    assertEquals(expected.get(WAVING), response.get(WAVING));
    assertEquals(expected.get(PACKING), response.get(PACKING));
  }

  @Test
  void mapThroughput() {

    final var throughput = List.of(
        new Throughput(DATE_FROM, List.of(
            new Throughput.QuantityByProcessName(WAVING, 500),
            new Throughput.QuantityByProcessName(PICKING, 500)
        )),
        new Throughput(DATE_FROM.plus(1, ChronoUnit.HOURS), List.of(
            new Throughput.QuantityByProcessName(WAVING, 100),
            new Throughput.QuantityByProcessName(PICKING, 100)
        ))
    );

    final BacklogProjection backlogProjection = new BacklogProjection(
        null,
        null,
        throughput,
        DATE_FROM,
        DATE_TO,
        Workflow.FBM_WMS_OUTBOUND
    );

    final var expected = Map.of(
        PICKING, Map.of(
            DATE_FROM, 500L,
            DATE_FROM.plus(1, ChronoUnit.HOURS), 100L
        ),
        WAVING, Map.of(
            DATE_FROM, 500L,
            DATE_FROM.plus(1, ChronoUnit.HOURS), 100L
        )
    );

    final var response = backlogProjection.mapThroughput();
    assertNotNull(response);
    assertEquals(expected, response);
  }
}
