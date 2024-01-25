package com.mercadolibre.planning.model.api.service;

import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.service.sla.OutboundSlaPropertiesService;
import com.mercadolibre.planning.model.api.domain.service.sla.SlaProperties;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeService;
import com.mercadolibre.planning.model.api.domain.usecase.sla.GetSlaByWarehouseOutboundService;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OutboundProjectionResultServiceTest {

  private static final List<ZonedDateTime> SLAS = List.of(
      ZonedDateTime.parse("2022-08-22T12:00:00Z"),
      ZonedDateTime.parse("2022-08-22T13:00:00Z"),
      ZonedDateTime.parse("2022-08-22T14:00:00Z"),
      ZonedDateTime.parse("2022-08-22T15:00:00Z"),
      ZonedDateTime.parse("2022-08-22T16:00:00Z")
  );

  private static final String TIME_ZONE = "UTC";

  @InjectMocks
  private OutboundSlaPropertiesService useCase;

  @Mock
  private GetSlaByWarehouseOutboundService getSlaByWarehouseOutboundService;

  @Mock
  private GetCycleTimeService getCycleTimeService;

  private static List<Instant> defaults() {
    return SLAS.subList(1, 3)
        .stream()
        .map(ZonedDateTime::toInstant)
        .collect(Collectors.toList());
  }

  @Test
  void testExecute() {
    // GIVEN
    final var input = new OutboundSlaPropertiesService.Input(
        WAREHOUSE_ID,
        Workflow.FBM_WMS_OUTBOUND,
        defaults()
    );

    mockCycleTimes();

    // WHEN
    final var result = useCase.get(input);

    // THEN
    assertNotNull(result);
    assertEquals(5, result.size());
    assertSla(0, 10L, result);
    assertSla(1, 20L, result);
    assertSla(2, 30L, result);
    assertSla(3, 40L, result);
    assertSla(4, 50L, result);
  }

  @Test
  void testCycleTimeServiceError() {
    // GIVEN
    final var input = new OutboundSlaPropertiesService.Input(
        WAREHOUSE_ID,
        Workflow.FBM_WMS_OUTBOUND,
        defaults()
    );

    when(getCycleTimeService.execute(any())).thenThrow(RuntimeException.class);

    // WHEN
    assertThrows(RuntimeException.class, () -> useCase.get(input));
  }

  private void assertSla(final int slaIndex, final long expected, final Map<Instant, SlaProperties> results) {
    final var sla = SLAS.get(slaIndex).toInstant();
    assertEquals(expected, results.get(sla).cycleTime());
  }

  private void mockGetSlasWarehouseOutboundService() {
    final var input = new GetSlaByWarehouseInput(
        WAREHOUSE_ID,
        SLAS.get(0),
        SLAS.get(4),
        SLAS.subList(1, 3),
        TIME_ZONE
    );

    when(getSlaByWarehouseOutboundService.execute(input))
        .thenReturn(
            List.of(
                GetSlaByWarehouseOutput.builder()
                    .date(SLAS.get(0))
                    .build(),
                GetSlaByWarehouseOutput.builder()
                    .date(SLAS.get(1))
                    .build(),
                GetSlaByWarehouseOutput.builder()
                    .date(SLAS.get(2))
                    .build(),
                GetSlaByWarehouseOutput.builder()
                    .date(SLAS.get(3))
                    .build(),
                GetSlaByWarehouseOutput.builder()
                    .date(SLAS.get(4))
                    .build()
            )
        );
  }

  private void mockCycleTimes() {
    final var input = new GetCycleTimeInput(WAREHOUSE_ID, SLAS.subList(1, 3));

    when(getCycleTimeService.execute(input))
        .thenReturn(
            Map.of(
                SLAS.get(0), 10L,
                SLAS.get(1), 20L,
                SLAS.get(2), 30L,
                SLAS.get(3), 40L,
                SLAS.get(4), 50L
            )
        );
  }
}
