package com.mercadolibre.planning.model.api.usecase.projection.backlog.calculate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionService;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionService.IncomingBacklog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionService.Throughput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.helper.BacklogBySlaHelper;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.BacklogBySla;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.PlannedBacklogBySla;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.ThroughputByHour;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.UpstreamBacklog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.ProjectionResult;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.QuantityAtDate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CalculateBacklogProjectionAdapterTest {
  private static final Instant DATE_FROM = Instant.parse("2022-03-31T12:00:00Z");

  private static final Instant DATE_BETWEEN = Instant.parse("2022-03-31T13:00:00Z");

  private static final Instant DATE_TO = Instant.parse("2022-03-31T14:00:00Z");

  private static final Instant SLA_A = Instant.parse("2022-03-31T16:00:00Z");

  private static final Instant SLA_B = Instant.parse("2022-03-31T17:00:00Z");

  private static final Instant SLA_C = Instant.parse("2022-03-31T18:00:00Z");

  private static final Instant SLA_D = Instant.parse("2022-03-31T19:00:00Z");

  private static IncomingBacklog<BacklogBySla> incomingBacklog() {
    final var firstOpHour = List.of(
        new QuantityAtDate(SLA_A, 10),
        new QuantityAtDate(SLA_B, 15),
        new QuantityAtDate(SLA_C, 20)
    );

    final var secondOpHour = List.of(
        new QuantityAtDate(SLA_B, 20),
        new QuantityAtDate(SLA_C, 35)
    );

    final var thirdOpHour = List.of(
        new QuantityAtDate(SLA_B, 20),
        new QuantityAtDate(SLA_C, 30),
        new QuantityAtDate(SLA_D, 40)
    );

    return new PlannedBacklogBySla(
        Map.of(
            DATE_FROM, new BacklogBySla(firstOpHour),
            DATE_BETWEEN, new BacklogBySla(secondOpHour),
            DATE_TO, new BacklogBySla(thirdOpHour)
        )
    );
  }

  private static BacklogBySla initialBacklog() {
    return new BacklogBySla(
        List.of(
            new QuantityAtDate(SLA_A, 5),
            new QuantityAtDate(SLA_B, 10)
        )
    );
  }

  private static Throughput throughput() {
    return new ThroughputByHour(
        Map.of(
            DATE_FROM, 50,
            DATE_BETWEEN, 50,
            DATE_TO, 50
        )
    );
  }

  private static void assertFirstOperatingHourValues(final ProjectionResult<BacklogBySla> projectionResult) {
    final var state = projectionResult.getResultingState();
    final var processed = state.getProcessed().getDistributions();
    assertEquals(3, processed.size());
    assertEquals(SLA_A, processed.get(0).getDate());
    assertEquals(15, processed.get(0).getQuantity());
    assertEquals(SLA_B, processed.get(1).getDate());
    assertEquals(25, processed.get(1).getQuantity());
    assertEquals(SLA_C, processed.get(2).getDate());
    assertEquals(10, processed.get(2).getQuantity());

    final var carryOver = state.getCarryOver().getDistributions();
    assertEquals(3, carryOver.size());
    assertEquals(SLA_A, carryOver.get(0).getDate());
    assertEquals(0, carryOver.get(0).getQuantity());
    assertEquals(SLA_B, carryOver.get(1).getDate());
    assertEquals(0, carryOver.get(1).getQuantity());
    assertEquals(SLA_C, carryOver.get(2).getDate());
    assertEquals(10, carryOver.get(2).getQuantity());
  }

  private static void assertSecondOperatingHourValues(final ProjectionResult<BacklogBySla> projectionResult) {
    final var state = projectionResult.getResultingState();
    final var processed = state.getProcessed().getDistributions();
    assertEquals(2, processed.size());
    assertEquals(SLA_B, processed.get(0).getDate());
    assertEquals(20, processed.get(0).getQuantity());
    assertEquals(SLA_C, processed.get(1).getDate());
    assertEquals(30, processed.get(1).getQuantity());

    final var carryOver = state.getCarryOver().getDistributions();
    assertEquals(2, carryOver.size());
    assertEquals(SLA_B, carryOver.get(0).getDate());
    assertEquals(0, carryOver.get(0).getQuantity());
    assertEquals(SLA_C, carryOver.get(1).getDate());
    assertEquals(15, carryOver.get(1).getQuantity());
  }

  @Test
  @DisplayName("test backlog projection when current datetime is o'clock")
  public void testBacklogProjectionByCpt() {
    // GIVEN
    final var helper = new BacklogBySlaHelper();

    // WHEN
    final List<ProjectionResult<BacklogBySla>> result = CalculateBacklogProjectionService.project(
        DATE_FROM,
        DATE_TO,
        incomingBacklog(),
        initialBacklog(),
        throughput(),
        helper
    );

    // THEN
    assertNotNull(result);
    assertEquals(2, result.size());


    assertEquals(DATE_FROM, result.get(0).getOperatingHour());
    assertFirstOperatingHourValues(result.get(0));

    assertEquals(DATE_BETWEEN, result.get(1).getOperatingHour());
    assertSecondOperatingHourValues(result.get(1));
  }

  @Test
  @DisplayName("test backlog projection when current datetime is and half")
  public void testBacklogProjectionByCptAndHalf() {
    // GIVEN
    final var helper = new BacklogBySlaHelper();

    final var dateFrom = DATE_FROM.plus(30, ChronoUnit.MINUTES);

    final var throughput = new ThroughputByHour(
        Map.of(
            DATE_FROM, 100,
            DATE_BETWEEN, 50,
            DATE_TO, 50
        )
    );

    final var incomingBacklog = new PlannedBacklogBySla(
        Map.of(
            DATE_FROM, new BacklogBySla(List.of(
                new QuantityAtDate(SLA_A, 20),
                new QuantityAtDate(SLA_B, 30),
                new QuantityAtDate(SLA_C, 40)
            )),
            DATE_BETWEEN, new BacklogBySla(List.of(
                new QuantityAtDate(SLA_B, 20),
                new QuantityAtDate(SLA_C, 35)
            )),
            DATE_TO, new BacklogBySla(List.of(
                new QuantityAtDate(SLA_B, 20),
                new QuantityAtDate(SLA_C, 30),
                new QuantityAtDate(SLA_D, 40)
            ))
        )
    );

    // WHEN
    final List<ProjectionResult<BacklogBySla>> result = CalculateBacklogProjectionService.project(
        dateFrom,
        DATE_TO,
        incomingBacklog,
        initialBacklog(),
        throughput,
        helper
    );

    // THEN
    assertNotNull(result);
    assertEquals(2, result.size());

    assertEquals(dateFrom, result.get(0).getOperatingHour());
    assertFirstOperatingHourValues(result.get(0));

    assertEquals(DATE_BETWEEN, result.get(1).getOperatingHour());
    assertSecondOperatingHourValues(result.get(1));
  }

  @Test
  @DisplayName("test backlog projection with upstream backlog")
  public void testBacklogProjectionByCptWithUpstreamBacklog() {
    // GIVEN
    final var helper = new BacklogBySlaHelper();

    final var dateFrom = DATE_FROM.plus(30, ChronoUnit.MINUTES);

    final var throughput = new ThroughputByHour(
        Map.of(
            DATE_FROM, 100,
            DATE_BETWEEN, 50,
            DATE_TO, 50
        )
    );

    final var upstreamBacklog = new UpstreamBacklog(
        Map.of(
            dateFrom, new BacklogBySla(List.of(
                new QuantityAtDate(SLA_A, 10),
                new QuantityAtDate(SLA_B, 15),
                new QuantityAtDate(SLA_C, 20)
            )),
            DATE_BETWEEN, new BacklogBySla(List.of(
                new QuantityAtDate(SLA_B, 20),
                new QuantityAtDate(SLA_C, 35)
            )),
            DATE_TO, new BacklogBySla(List.of(
                new QuantityAtDate(SLA_B, 20),
                new QuantityAtDate(SLA_C, 30),
                new QuantityAtDate(SLA_D, 40)
            ))
        )
    );

    // WHEN
    final List<ProjectionResult<BacklogBySla>> result = CalculateBacklogProjectionService.project(
        dateFrom,
        DATE_TO,
        upstreamBacklog,
        initialBacklog(),
        throughput,
        helper
    );

    // THEN
    assertNotNull(result);
    assertEquals(2, result.size());

    assertEquals(dateFrom, result.get(0).getOperatingHour());
    assertFirstOperatingHourValues(result.get(0));

    assertEquals(DATE_BETWEEN, result.get(1).getOperatingHour());
    assertSecondOperatingHourValues(result.get(1));
  }
}
