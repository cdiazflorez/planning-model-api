package com.mercadolibre.planning.model.api.usecase.projection.backlog;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BacklogProjectionBySla;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.BacklogBySla;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.PlannedBacklogBySla;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.ThroughputByHour;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.ProjectionResult;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.QuantityAtDate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class BacklogProjectionBySlaTest {
  private static final Instant OP_HOUR_A = Instant.parse("2022-03-31T12:00:00Z");

  private static final Instant OP_HOUR_B = Instant.parse("2022-03-31T13:00:00Z");

  private static final Instant OP_HOUR_C = Instant.parse("2022-03-31T14:00:00Z");

  private static final Instant SLA_A = Instant.parse("2022-03-31T16:00:00Z");

  private static final Instant SLA_B = Instant.parse("2022-03-31T17:00:00Z");

  private static final Instant SLA_C = Instant.parse("2022-03-31T18:00:00Z");

  private static final Instant SLA_D = Instant.parse("2022-03-31T19:00:00Z");

  @Test
  public void testOutboundProjections() {
    // GIVEN
    final var target = new BacklogProjectionBySla();

    // WHEN
    final var result = target.execute(
        OP_HOUR_A,
        OP_HOUR_C,
        FBM_WMS_OUTBOUND,
        List.of(WAVING, PICKING, PACKING),
        outboundThroughput(),
        outboundPlannedUnits(),
        outboundCurrentBacklog()
    );

    // THEN
    assertNotNull(result);

    assertWavingResults(result.get(WAVING));
    assertPickingResults(result.get(PICKING));
    assertPackingResults(result.get(PACKING));
  }

  private Map<ProcessName, ThroughputByHour> outboundThroughput() {
    final ThroughputByHour wavingTph = new ThroughputByHour(
        Map.of(
            OP_HOUR_A, 100,
            OP_HOUR_B, 130,
            OP_HOUR_C, 300
        )
    );

    final ThroughputByHour pickingTph = new ThroughputByHour(
        Map.of(
            OP_HOUR_A, 100,
            OP_HOUR_B, 200,
            OP_HOUR_C, 300
        )
    );

    final ThroughputByHour packingTph = new ThroughputByHour(
        Map.of(
            OP_HOUR_A, 225,
            OP_HOUR_B, 250,
            OP_HOUR_C, 250
        )
    );

    return Map.of(
        WAVING, wavingTph,
        PICKING, pickingTph,
        PACKING, packingTph
    );
  }

  private PlannedBacklogBySla outboundPlannedUnits() {
    final var opHourA = List.of(
        new QuantityAtDate(SLA_A, 20),
        new QuantityAtDate(SLA_B, 40)
    );

    final var opHourB = List.of(
        new QuantityAtDate(SLA_B, 50),
        new QuantityAtDate(SLA_C, 60),
        new QuantityAtDate(SLA_D, 20)
    );

    final var opHourC = List.of(
        new QuantityAtDate(SLA_C, 120),
        new QuantityAtDate(SLA_D, 170)
    );

    return new PlannedBacklogBySla(
        Map.of(
            OP_HOUR_A, new BacklogBySla(opHourA),
            OP_HOUR_B, new BacklogBySla(opHourB),
            OP_HOUR_C, new BacklogBySla(opHourC)
        )
    );
  }

  private Map<ProcessName, BacklogBySla> outboundCurrentBacklog() {
    final var waving = List.of(
        new QuantityAtDate(SLA_A, 50),
        new QuantityAtDate(SLA_B, 70)
    );

    final var picking = List.of(
        new QuantityAtDate(SLA_A, 75),
        new QuantityAtDate(SLA_B, 80)
    );

    final var packing = List.of(
        new QuantityAtDate(SLA_B, 110),
        new QuantityAtDate(SLA_C, 75)
    );

    return Map.of(
      WAVING, new BacklogBySla(waving),
      PICKING, new BacklogBySla(picking),
      PACKING, new BacklogBySla(packing)
    );
  }

  private void assertWavingResults(final List<ProjectionResult<BacklogBySla>> projectionResults) {
    assertEquals(2, projectionResults.size());

    final var firstHour = projectionResults.get(0);

    final var firstHourProcessed = firstHour.getResultingState().getProcessed().getDistributions();
    assertEquals(70, firstHourProcessed.get(0).getQuantity());
    assertEquals(30, firstHourProcessed.get(1).getQuantity());

    final var firstHourCarryOver = firstHour.getResultingState().getCarryOver().getDistributions();
    assertEquals(0, firstHourCarryOver.get(0).getQuantity());
    assertEquals(80, firstHourCarryOver.get(1).getQuantity());

    final var secondHour = projectionResults.get(1);

    final var secondHourProcessed = secondHour.getResultingState().getProcessed().getDistributions();
    assertEquals(130, secondHourProcessed.get(0).getQuantity());
    assertEquals(0, secondHourProcessed.get(1).getQuantity());
    assertEquals(0, secondHourProcessed.get(2).getQuantity());

    final var secondHourCarryOver = secondHour.getResultingState().getCarryOver().getDistributions();
    assertEquals(0, secondHourCarryOver.get(0).getQuantity());
    assertEquals(60, secondHourCarryOver.get(1).getQuantity());
    assertEquals(20, secondHourCarryOver.get(2).getQuantity());
  }

  private void assertPickingResults(final List<ProjectionResult<BacklogBySla>> projectionResults) {
    assertEquals(2, projectionResults.size());

    final var firstHour = projectionResults.get(0);

    final var firstHourProcessed = firstHour.getResultingState().getProcessed().getDistributions();
    assertEquals(100, firstHourProcessed.get(0).getQuantity());
    assertEquals(0, firstHourProcessed.get(1).getQuantity());

    final var firstHourCarryOver = firstHour.getResultingState().getCarryOver().getDistributions();
    assertEquals(45, firstHourCarryOver.get(0).getQuantity());
    assertEquals(110, firstHourCarryOver.get(1).getQuantity());

    final var secondHour = projectionResults.get(1);

    final var secondHourProcessed = secondHour.getResultingState().getProcessed().getDistributions();
    assertEquals(45, secondHourProcessed.get(0).getQuantity());
    assertEquals(155, secondHourProcessed.get(1).getQuantity());

    final var secondHourCarryOver = secondHour.getResultingState().getCarryOver().getDistributions();
    assertEquals(0, secondHourCarryOver.get(0).getQuantity());
    assertEquals(85, secondHourCarryOver.get(1).getQuantity());
  }

  private void assertPackingResults(final List<ProjectionResult<BacklogBySla>> projectionResults) {
    assertEquals(2, projectionResults.size());

    final var firstHour = projectionResults.get(0);

    final var firstHourProcessed = firstHour.getResultingState().getProcessed().getDistributions();
    assertEquals(100, firstHourProcessed.get(0).getQuantity());
    assertEquals(110, firstHourProcessed.get(1).getQuantity());
    assertEquals(15, firstHourProcessed.get(2).getQuantity());

    final var firstHourCarryOver = firstHour.getResultingState().getCarryOver().getDistributions();
    assertEquals(0, firstHourCarryOver.get(0).getQuantity());
    assertEquals(0, firstHourCarryOver.get(1).getQuantity());
    assertEquals(60, firstHourCarryOver.get(2).getQuantity());

    final var secondHour = projectionResults.get(1);

    final var secondHourProcessed = secondHour.getResultingState().getProcessed().getDistributions();
    assertEquals(45, secondHourProcessed.get(0).getQuantity());
    assertEquals(155, secondHourProcessed.get(1).getQuantity());
    assertEquals(50, secondHourProcessed.get(2).getQuantity());

    final var secondHourCarryOver = secondHour.getResultingState().getCarryOver().getDistributions();
    assertEquals(0, secondHourCarryOver.get(0).getQuantity());
    assertEquals(0, secondHourCarryOver.get(1).getQuantity());
    assertEquals(10, secondHourCarryOver.get(2).getQuantity());
  }
}
