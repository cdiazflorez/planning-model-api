package com.mercadolibre.planning.model.api.projection.waverless;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.AMBIENT;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.SLA_1;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.SLA_2;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.SLA_3;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PendingBacklogTest {


  static Stream<Arguments> source() {
    return Stream.of(
        Arguments.of(
            Map.of(
                TOT_MONO, List.of(
                    new PendingBacklog.AvailableBacklog(SLA_1, SLA_1, 5D),
                    new PendingBacklog.AvailableBacklog(SLA_2, SLA_2, 5D)
                ),
                AMBIENT, List.of(
                    new PendingBacklog.AvailableBacklog(SLA_3, SLA_3, 5D)
                )
            ),
            Map.of(
                TOT_MONO, List.of(
                    new PendingBacklog.AvailableBacklog(SLA_3, SLA_3, 5D)
                ),
                AMBIENT, List.of(
                    new PendingBacklog.AvailableBacklog(SLA_2, SLA_2, 5D)
                )
            ),
            Map.of(
                TOT_MONO, Set.of(SLA_1, SLA_2, SLA_3),
                AMBIENT, Set.of(SLA_2, SLA_3)
            )
        ),
        Arguments.of(
            Map.of(
                TOT_MONO, List.of(
                    new PendingBacklog.AvailableBacklog(SLA_1, SLA_1, 5D),
                    new PendingBacklog.AvailableBacklog(SLA_2, SLA_2, 5D)
                )
            ),
            Map.of(
                AMBIENT, List.of(
                    new PendingBacklog.AvailableBacklog(SLA_2, SLA_2, 5D),
                    new PendingBacklog.AvailableBacklog(SLA_3, SLA_3, 5D)
                )
            ),
            Map.of(
                TOT_MONO, Set.of(SLA_1, SLA_2),
                AMBIENT, Set.of(SLA_2, SLA_3)
            )
        )
    );
  }

  @ParameterizedTest
  @MethodSource(value = "source")
  void testCalculateSlasByProcessPath(
      final Map<ProcessPath, List<PendingBacklog.AvailableBacklog>> readyToWave,
      final Map<ProcessPath, List<PendingBacklog.AvailableBacklog>> forecast,
      final Map<ProcessPath, Set<Instant>> expected
  ) {
    // GIVEN
    final var pendingBacklog = new PendingBacklog(readyToWave, forecast);

    // WHEN
    final var actual = pendingBacklog.calculateSlasByProcessPath();

    // THEN
    assertEquals(expected, actual);
  }
}
