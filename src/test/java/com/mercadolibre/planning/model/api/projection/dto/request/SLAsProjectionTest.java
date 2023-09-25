package com.mercadolibre.planning.model.api.projection.dto.request;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.SLAsProjectionRequest.Backlog;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.SLAsProjectionRequest.Backlog.Process.ProcessPathByDateOut;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.SLAsProjectionRequest.Backlog.Process.ProcessPathByDateOut.QuantityByDateOut;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.projection.request.SLAsProjectionRequest;
import com.mercadolibre.planning.model.api.web.controller.projection.request.SLAsProjectionRequest.PlannedUnit.ProcessPathByDateInOut;
import com.mercadolibre.planning.model.api.web.controller.projection.request.SLAsProjectionRequest.PlannedUnit.ProcessPathByDateInOut.QuantityByDateInOut;
import com.mercadolibre.planning.model.api.web.controller.projection.request.SLAsProjectionRequest.Throughput.QuantityByProcessName;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class SLAsProjectionTest {

  private static final Instant SLAS_1 = Instant.parse("2023-09-22T10:00:00Z");
  private static final Instant SLAS_2 = Instant.parse("2023-09-22T11:00:00Z");
  private static final Instant SLAS_3 = Instant.parse("2023-09-22T12:00:00Z");
  private static final Instant SLAS_4 = Instant.parse("2023-09-22T13:00:00Z");
  private static final Instant SLAS_5 = Instant.parse("2023-09-22T14:00:00Z");
  private static final Instant DATE_1 = Instant.parse("2023-09-22T00:00:00Z");
  private static final Instant DATE_2 = Instant.parse("2023-09-22T01:00:00Z");
  private static final Instant DATE_3 = Instant.parse("2023-09-22T02:00:00Z");
  private static final Instant DATE_4 = Instant.parse("2023-09-22T03:00:00Z");
  private static final Instant DATE_5 = Instant.parse("2023-09-22T04:00:00Z");
  private static final Instant DATE_6 = Instant.parse("2023-09-22T05:00:00Z");
  private static final Instant DATE_7 = Instant.parse("2023-09-22T06:00:00Z");

  private static final Backlog BACKLOG = new Backlog(
      Set.of(
          new Backlog.Process(
              PICKING, Set.of(
              new ProcessPathByDateOut(
                  TOT_MONO, Set.of(
                  new QuantityByDateOut(SLAS_1, 1000L),
                  new QuantityByDateOut(SLAS_2, 1000L)
              )
              ),
              new ProcessPathByDateOut(
                  NON_TOT_MONO, Set.of(
                  new QuantityByDateOut(SLAS_1, 250L)
              )
              ),
              new ProcessPathByDateOut(
                  TOT_MULTI_BATCH, Set.of(
                  new QuantityByDateOut(SLAS_2, 500L)
              )
              )
          )
          ),
          new Backlog.Process(
              PACKING, Set.of(
              new ProcessPathByDateOut(
                  TOT_MONO, Set.of(
                  new QuantityByDateOut(SLAS_3, 1000L),
                  new QuantityByDateOut(SLAS_4, 500L)
              )
              ),
              new ProcessPathByDateOut(
                  NON_TOT_MONO, Set.of(
                  new QuantityByDateOut(SLAS_5, 100L)
              )
              )
          )
          ),
          new Backlog.Process(
              BATCH_SORTER, Set.of(
              new ProcessPathByDateOut(
                  TOT_MULTI_BATCH, Set.of(
                  new QuantityByDateOut(SLAS_1, 1000L),
                  new QuantityByDateOut(SLAS_3, 500L)
              )
              )
          )
          ),
          new Backlog.Process(
              WALL_IN, Set.of()
          )
      )
  );

  private static final SLAsProjectionRequest.PlannedUnit PLANNED_UNIT = new SLAsProjectionRequest.PlannedUnit(
      Set.of(
          new ProcessPathByDateInOut(
              TOT_MONO, Set.of(
              new QuantityByDateInOut(DATE_1, SLAS_1, 240L)
          )
          ),
          new ProcessPathByDateInOut(
              TOT_MULTI_BATCH, Set.of(
              new QuantityByDateInOut(DATE_2, SLAS_2, 1200L)
          )
          ),
          new ProcessPathByDateInOut(
              NON_TOT_MONO, Set.of(
              new QuantityByDateInOut(DATE_6, SLAS_5, 1800L)
          )
          )
      )
  );

  private static final Set<SLAsProjectionRequest.Throughput> THROUGHPUT = Set.of(
      new SLAsProjectionRequest.Throughput(
          DATE_1, Set.of(
          new QuantityByProcessName(PICKING, 1000),
          new QuantityByProcessName(PACKING, 1000),
          new QuantityByProcessName(BATCH_SORTER, 1000),
          new QuantityByProcessName(WALL_IN, 1000),
          new QuantityByProcessName(PACKING_WALL, 1000))
      ),
      new SLAsProjectionRequest.Throughput(
          DATE_2, Set.of(
          new QuantityByProcessName(PICKING, 1000),
          new QuantityByProcessName(PACKING, 1000),
          new QuantityByProcessName(BATCH_SORTER, 1000),
          new QuantityByProcessName(WALL_IN, 1000),
          new QuantityByProcessName(PACKING_WALL, 1000))
      ),
      new SLAsProjectionRequest.Throughput(
          DATE_3, Set.of(
          new QuantityByProcessName(PICKING, 1000),
          new QuantityByProcessName(PACKING, 1000),
          new QuantityByProcessName(BATCH_SORTER, 1000),
          new QuantityByProcessName(WALL_IN, 1000),
          new QuantityByProcessName(PACKING_WALL, 1000))
      ),
      new SLAsProjectionRequest.Throughput(
          DATE_4, Set.of(
          new QuantityByProcessName(PICKING, 1000),
          new QuantityByProcessName(PACKING, 1000),
          new QuantityByProcessName(BATCH_SORTER, 1000),
          new QuantityByProcessName(WALL_IN, 1000),
          new QuantityByProcessName(PACKING_WALL, 1000))
      ),
      new SLAsProjectionRequest.Throughput(
          DATE_5, Set.of(
          new QuantityByProcessName(PICKING, 1000),
          new QuantityByProcessName(PACKING, 1000),
          new QuantityByProcessName(BATCH_SORTER, 1000),
          new QuantityByProcessName(WALL_IN, 1000),
          new QuantityByProcessName(PACKING_WALL, 1000))
      ),
      new SLAsProjectionRequest.Throughput(
          DATE_6, Set.of(
          new QuantityByProcessName(PICKING, 1000),
          new QuantityByProcessName(PACKING, 1000),
          new QuantityByProcessName(BATCH_SORTER, 1000),
          new QuantityByProcessName(WALL_IN, 1000),
          new QuantityByProcessName(PACKING_WALL, 1000))
      ),
      new SLAsProjectionRequest.Throughput(
          DATE_7, Set.of(
          new QuantityByProcessName(PICKING, 1000),
          new QuantityByProcessName(PACKING, 1000),
          new QuantityByProcessName(BATCH_SORTER, 1000),
          new QuantityByProcessName(WALL_IN, 1000),
          new QuantityByProcessName(PACKING_WALL, 1000))
      )
  );

  private static final Map<Instant, Integer> CYCLE_TIME_BY_SLA = Map.of(
      DATE_1, 30,
      DATE_2, 30,
      DATE_3, 30,
      DATE_4, 30,
      DATE_5, 30,
      DATE_6, 30,
      DATE_7, 30
  );

  private static final SLAsProjectionRequest PROJECTION_REQUEST = new SLAsProjectionRequest(
      Workflow.FBM_WMS_OUTBOUND,
      DATE_1,
      DATE_7,
      BACKLOG,
      PLANNED_UNIT,
      THROUGHPUT,
      CYCLE_TIME_BY_SLA
  );

  private static Map<Instant, Integer> throughputValues() {
    return Map.of(
        DATE_1, 1000,
        DATE_2, 1000,
        DATE_3, 1000,
        DATE_4, 1000,
        DATE_5, 1000,
        DATE_6, 1000,
        DATE_7, 1000
    );
  }

  @Test
  void mapForecast() {
    final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> expected = Map.of(
        DATE_1, Map.of(
            TOT_MONO, Map.of(
                SLAS_1, 240L
            )
        ),
        DATE_2, Map.of(
            TOT_MULTI_BATCH, Map.of(
                SLAS_2, 1200L
            )
        ),
        DATE_6, Map.of(
            NON_TOT_MONO, Map.of(
                SLAS_5, 1800L
            )
        )
    );

    final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> response = PROJECTION_REQUEST.mapForecast();

    assertNotNull(response);
    assertEquals(expected, response);
  }

  @Test
  void mapBacklog() {
    final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> expected = Map.of(
        PICKING, Map.of(
            TOT_MONO, Map.of(SLAS_1, 1000L, SLAS_2, 1000L),
            NON_TOT_MONO, Map.of(SLAS_1, 250L),
            TOT_MULTI_BATCH, Map.of(SLAS_2, 500L)
        ),
        PACKING, Map.of(
            TOT_MONO, Map.of(SLAS_3, 1000L, SLAS_4, 500L),
            NON_TOT_MONO, Map.of(SLAS_5, 100L)
        ),
        BATCH_SORTER, Map.of(
            TOT_MULTI_BATCH, Map.of(SLAS_1, 1000L, SLAS_3, 500L)
        ),
        WALL_IN, Map.of()
    );

    final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> response = PROJECTION_REQUEST.mapBacklogs();

    assertNotNull(response);
    assertEquals(expected.get(PACKING), response.get(PACKING));
    assertEquals(expected.get(PICKING), response.get(PICKING));
    assertEquals(expected.get(BATCH_SORTER), response.get(BATCH_SORTER));
    assertEquals(expected.get(WALL_IN), response.get(WALL_IN));
  }

  @Test
  void mapThroughput() {
    final Map<ProcessName, Map<Instant, Integer>> expected = Map.of(
        PICKING, throughputValues(),
        PACKING, throughputValues(),
        BATCH_SORTER, throughputValues(),
        WALL_IN, throughputValues(),
        PACKING_WALL, throughputValues()
    );

    final Map<ProcessName, Map<Instant, Integer>> response = PROJECTION_REQUEST.mapThroughput();

    assertNotNull(response);
    assertEquals(expected, response);
  }
}
