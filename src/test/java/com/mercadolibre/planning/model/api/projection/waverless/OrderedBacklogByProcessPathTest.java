package com.mercadolibre.planning.model.api.projection.waverless;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_ORDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadolibre.flow.projection.tools.services.entities.context.Backlog;
import java.util.Collections;
import java.util.Map;
import java.util.function.LongUnaryOperator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OrderedBacklogByProcessPathTest {

  @Test
  @DisplayName("get total on empty backlog must return zero")
  void testTotalOnEmptyBacklog() {
    // GIVEN
    final var backlog = new OrderedBacklogByProcessPath(Collections.emptyMap());

    // WHEN
    final var total = backlog.total();

    // THEN
    assertEquals(0L, total);
  }

  @Test
  @DisplayName("get total on must take into account all process paths")
  void testTotal() {
    // GIVEN
    final Backlog totMono = Mockito.mock(Backlog.class);
    final Backlog totMultiOrder = Mockito.mock(Backlog.class);
    final Backlog totMultiBatch = Mockito.mock(Backlog.class);

    when(totMono.total()).thenReturn(100L);
    when(totMultiOrder.total()).thenReturn(120L);
    when(totMultiBatch.total()).thenReturn(140L);

    final var backlog = new OrderedBacklogByProcessPath(
        Map.of(
            TOT_MONO, totMono,
            TOT_MULTI_ORDER, totMultiOrder,
            TOT_MULTI_BATCH, totMultiBatch
        )
    );

    // WHEN
    final var total = backlog.total();

    // THEN
    assertEquals(360L, total);
  }

  @Test
  @DisplayName("map function should be applied to the backlogs of all process paths")
  void testMap() {
    // GIVEN
    final LongUnaryOperator op = LongUnaryOperator.identity();

    final Backlog totMono = Mockito.mock(Backlog.class);
    final Backlog totMultiOrder = Mockito.mock(Backlog.class);
    final Backlog totMultiBatch = Mockito.mock(Backlog.class);

    when(totMono.map(op)).thenReturn(totMono);
    when(totMultiOrder.map(op)).thenReturn(totMultiOrder);
    when(totMultiBatch.map(op)).thenReturn(totMultiBatch);


    final var backlog = new OrderedBacklogByProcessPath(
        Map.of(
            TOT_MONO, totMono,
            TOT_MULTI_ORDER, totMultiOrder,
            TOT_MULTI_BATCH, totMultiBatch
        )
    );

    // WHEN
    backlog.map(op);

    // THEN
    verify(totMono).map(op);
    verify(totMultiOrder).map(op);
    verify(totMultiBatch).map(op);
  }
}
