package com.mercadolibre.planning.model.api.projection.waverless.idleness;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.projection.BacklogProjection.CONSOLIDATION_PROCESS_GROUP;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.FIRST_INFLECTION_POINT;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.LAST_INFLECTION_POINT;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.SLA_1;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.SLA_2;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.flow.projection.tools.services.entities.context.PiecewiseUpstream;
import com.mercadolibre.flow.projection.tools.services.entities.context.Upstream;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.backlogmanager.OrderedBacklogByProcessPath;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ProcessPathSplitterTest {

  private static long totalUnitsByProcess(final Map<String, Upstream> upstream, final String process) {
    return upstream.get(process)
        .calculateUpstreamUnitsForInterval(FIRST_INFLECTION_POINT, LAST_INFLECTION_POINT)
        .orElseThrow()
        .total();
  }

  static Stream<Arguments> parameters() {
    return Stream.of(
        Arguments.of(
            Map.of(
                TOT_MULTI_BATCH, Map.of(SLA_1, 100L, SLA_2, 300L),
                TOT_MONO, Map.of(SLA_1, 125L, SLA_2, 240L),
                NON_TOT_MONO, Map.of(SLA_1, 218L, SLA_2, 320L)
            ),
            903L,
            400L
        ),
        Arguments.of(
            emptyMap(), 0L, 0L
        )
    );
  }

  @ParameterizedTest
  @MethodSource("parameters")
  @DisplayName("test split backlog from several process paths into packing and packing wall")
  void test(
      final Map<ProcessPath, Map<Instant, Long>> backlog,
      final Long expectedPackingTotal,
      final Long expectedPackingWallTotal
  ) {
    // GIVEN
    final var splitter = new ProcessPathSplitter(OrderedBacklogByProcessPath::new);

    final var backlogToSplit = OrderedBacklogByProcessPath.from(backlog);

    final var emptyBacklog = new OrderedBacklogByProcessPath(emptyMap());

    final var upstream = new PiecewiseUpstream(Map.of(FIRST_INFLECTION_POINT, backlogToSplit, LAST_INFLECTION_POINT, emptyBacklog));

    // WHEN
    final var result = splitter.split(upstream);

    // THEN
    assertNotNull(result);

    assertTrue(result.containsKey(PACKING.getName()));
    final var totalPackingUnits = totalUnitsByProcess(result, PACKING.getName());
    assertEquals(expectedPackingTotal, totalPackingUnits);

    assertTrue(result.containsKey(CONSOLIDATION_PROCESS_GROUP));
    final var totalPackingWallUnits = totalUnitsByProcess(result, CONSOLIDATION_PROCESS_GROUP);
    assertEquals(expectedPackingWallTotal, totalPackingWallUnits);
  }
}
