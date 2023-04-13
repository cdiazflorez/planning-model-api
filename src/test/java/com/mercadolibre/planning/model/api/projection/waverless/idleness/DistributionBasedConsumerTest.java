package com.mercadolibre.planning.model.api.projection.waverless.idleness;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.SLA_1;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.SLA_2;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.SLA_3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDateConsumer;
import com.mercadolibre.planning.model.api.projection.waverless.OrderedBacklogByProcessPath;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DistributionBasedConsumerTest {
  private static final Instant STARTING_DATE = Instant.parse("2023-03-21T00:00:00Z");

  private static final Instant ENDING_DATE = Instant.parse("2023-03-21T00:05:00Z");

  @Test
  void testWithSeveralProcessPaths() {
    // GIVEN
    final var consumer = new DistributionBasedConsumer(new OrderedBacklogByDateConsumer());

    final var backlog = OrderedBacklogByProcessPath.from(
        Map.of(
            TOT_MONO, Map.of(SLA_1, 100L, SLA_2, 300L, SLA_3, 500L),
            TOT_MULTI_BATCH, Map.of(SLA_1, 200L, SLA_2, 200L, SLA_3, 200L)
        )
    );

    // WHEN
    final var result = consumer.consume(STARTING_DATE, ENDING_DATE, backlog, 500);

    // THEN
    assertNotNull(result);

    final OrderedBacklogByProcessPath consumed = (OrderedBacklogByProcessPath) (result.getConsumed());
    final var consumedBacklog = consumed.getBacklogs();
    assertEquals(300L, consumedBacklog.get(TOT_MONO).total());
    assertEquals(200L, consumedBacklog.get(TOT_MULTI_BATCH).total());
  }

  @Test
  void testWithSeveralProcessPathsAndWithoutBacklog() {
    // GIVEN
    final var consumer = new DistributionBasedConsumer(new OrderedBacklogByDateConsumer());

    final var backlog = OrderedBacklogByProcessPath.from(
        Map.of(
            TOT_MONO, Map.of(SLA_1, 100L, SLA_2, 300L, SLA_3, 500L),
            TOT_MULTI_BATCH, Map.of()
        )
    );

    // WHEN
    final var result = consumer.consume(STARTING_DATE, ENDING_DATE, backlog, 500);

    // THEN
    assertNotNull(result);

    final OrderedBacklogByProcessPath consumed = (OrderedBacklogByProcessPath) (result.getConsumed());
    final var consumedBacklog = consumed.getBacklogs();
    assertEquals(500L, consumedBacklog.get(TOT_MONO).total());
    assertEquals(0L, consumedBacklog.get(TOT_MULTI_BATCH).total());
  }

  @Test
  void testWithSeveralProcessPathsAndWithoutAnyBacklog() {
    // GIVEN
    final var consumer = new DistributionBasedConsumer(new OrderedBacklogByDateConsumer());

    final var backlog = OrderedBacklogByProcessPath.from(
        Map.of(
            TOT_MONO, Map.of(),
            TOT_MULTI_BATCH, Map.of()
        )
    );

    // WHEN
    final var result = consumer.consume(STARTING_DATE, ENDING_DATE, backlog, 500);

    // THEN
    assertNotNull(result);

    final OrderedBacklogByProcessPath consumed = (OrderedBacklogByProcessPath) (result.getConsumed());
    final var consumedBacklog = consumed.getBacklogs();
    assertEquals(0L, consumedBacklog.get(TOT_MONO).total());
    assertEquals(0L, consumedBacklog.get(TOT_MULTI_BATCH).total());
  }

  @Test
  void testWithSeveralProcessPathsWithoutProcessingPower() {
    // GIVEN
    final var consumer = new DistributionBasedConsumer(new OrderedBacklogByDateConsumer());

    final var backlog = OrderedBacklogByProcessPath.from(
        Map.of(
            TOT_MONO, Map.of(SLA_1, 100L, SLA_2, 300L, SLA_3, 500L),
            TOT_MULTI_BATCH, Map.of(SLA_1, 200L, SLA_2, 200L, SLA_3, 200L)
        )
    );

    // WHEN
    final var result = consumer.consume(STARTING_DATE, ENDING_DATE, backlog, 0);

    // THEN
    assertNotNull(result);

    final OrderedBacklogByProcessPath consumed = (OrderedBacklogByProcessPath) (result.getConsumed());
    final var consumedBacklog = consumed.getBacklogs();
    assertEquals(0L, consumedBacklog.get(TOT_MONO).total());
    assertEquals(0L, consumedBacklog.get(TOT_MULTI_BATCH).total());
  }
}
