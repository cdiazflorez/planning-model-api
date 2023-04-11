package com.mercadolibre.planning.model.api.projection.waverless.idleness;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mercadolibre.flow.projection.tools.services.entities.context.Backlog;
import com.mercadolibre.flow.projection.tools.services.entities.context.Merger;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDate;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDate.Quantity;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.helpers.OrderedBacklogByDateMerger;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.waverless.OrderedBacklogByProcessPath;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProcessPathMergerTest {

  private static final Instant[] SLAS = {
      Instant.parse("2023-03-29T00:00:00Z"),
      Instant.parse("2023-03-29T01:00:00Z"),
      Instant.parse("2023-03-29T02:00:00Z"),
      Instant.parse("2023-03-29T03:00:00Z"),
      Instant.parse("2023-03-29T04:00:00Z"),
      Instant.parse("2023-03-29T05:00:00Z"),
      Instant.parse("2023-03-29T06:00:00Z"),
  };

  private static final Backlog BACKLOG_1 = new OrderedBacklogByProcessPath(
      Map.of(
          ProcessPath.TOT_MONO, new OrderedBacklogByDate(
              Map.of(
                  SLAS[0], new Quantity(13),
                  SLAS[1], new Quantity(15)
              )
          ),
          ProcessPath.TOT_MULTI_ORDER, new OrderedBacklogByDate(
              Map.of(
                  SLAS[2], new Quantity(10),
                  SLAS[3], new Quantity(20)
              )
          )
      )
  );

  private static final Backlog BACKLOG_2 = new OrderedBacklogByProcessPath(
      Map.of(
          ProcessPath.TOT_MONO, new OrderedBacklogByDate(
              Map.of(
                  SLAS[1], new Quantity(23),
                  SLAS[2], new Quantity(25)
              )
          ),
          ProcessPath.TOT_MULTI_BATCH, new OrderedBacklogByDate(
              Map.of(
                  SLAS[2], new Quantity(10),
                  SLAS[3], new Quantity(20)
              )
          )
      )
  );

  private static final Backlog BACKLOG_3 = new OrderedBacklogByProcessPath(
      Map.of(
          ProcessPath.NON_TOT_MONO, new OrderedBacklogByDate(
              Map.of(
                  SLAS[2], new Quantity(101),
                  SLAS[3], new Quantity(201)
              )
          )
      )
  );

  private static final Backlog BACKLOG_4 = new OrderedBacklogByProcessPath(emptyMap());


  @Test
  void testMerge() {
    // GIVEN
    final Merger merger = new ProcessPathMerger(new OrderedBacklogByDateMerger());

    // WHEN
    final var mergedBacklog = merger.merge(BACKLOG_1, BACKLOG_2, BACKLOG_3, BACKLOG_4);

    // THEN
    assertNotNull(mergedBacklog);

    final var castedBacklog = (OrderedBacklogByProcessPath) mergedBacklog;
    final var backlogs = castedBacklog.getBacklogs();
    assertEquals(13 + 15 + 23 + 25, backlogs.get(ProcessPath.TOT_MONO).total());
    assertEquals(10 + 20, backlogs.get(ProcessPath.TOT_MULTI_ORDER).total());
    assertEquals(10 + 20, backlogs.get(ProcessPath.TOT_MULTI_BATCH).total());
    assertEquals(101L + 201, backlogs.get(ProcessPath.NON_TOT_MONO).total());
  }
}
