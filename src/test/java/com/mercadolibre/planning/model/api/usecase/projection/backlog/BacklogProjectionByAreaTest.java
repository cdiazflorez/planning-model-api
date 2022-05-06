package com.mercadolibre.planning.model.api.usecase.projection.backlog;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BacklogProjectionByArea;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BacklogProjectionBySla;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.helper.BacklogMapper;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.helper.UnitsAreaDistributionMapper;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.BacklogByArea;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.BacklogBySla;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.QuantityAtArea;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.ProjectionResult;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.QuantityAtDate;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.SimpleProcessedBacklog;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BacklogProjectionByAreaTest {

  private static final String NO_AREA = "undefined";

  private static final double ALLOWED_DELTA = 0.001;

  private static final Instant OP_HOUR_A = Instant.parse("2022-03-31T12:00:00Z");

  private static final Instant OP_HOUR_B = Instant.parse("2022-03-31T13:00:00Z");

  private static final Instant OP_HOUR_C = Instant.parse("2022-03-31T14:00:00Z");

  private static final Instant SLA_A = Instant.parse("2022-03-31T16:00:00Z");

  private static final Instant SLA_B = Instant.parse("2022-03-31T17:00:00Z");

  private static final Instant SLA_C = Instant.parse("2022-03-31T18:00:00Z");

  private static final Instant SLA_D = Instant.parse("2022-03-31T19:00:00Z");

  @InjectMocks
  private BacklogProjectionByArea backlogProjectionByArea;

  @Mock
  private BacklogProjectionBySla backlogProjectionBySla;

  @Test
  public void testExecute() {
    // GIVEN
    when(backlogProjectionBySla.execute(
        OP_HOUR_A,
        OP_HOUR_C,
        FBM_WMS_OUTBOUND,
        List.of(WAVING, PICKING, PACKING),
        null,
        null,
        null
    )).thenReturn(projectionResults());

    // WHEN
    final Map<ProcessName, List<ProjectionResult<BacklogByArea>>> result = backlogProjectionByArea.execute(
        OP_HOUR_A,
        OP_HOUR_C,
        FBM_WMS_OUTBOUND,
        List.of(WAVING, PICKING, PACKING),
        null,
        null,
        null,
        getBacklogMapper()
    );

    // THEN
    assertNotNull(result);

    assertWaving(result.get(WAVING));
    assertPicking(result.get(PICKING));
  }

  private void assertWaving(final List<ProjectionResult<BacklogByArea>> results) {
    final var firstOH = results.get(0);
    final var firstOhProcessed = firstOH.getResultingState().getProcessed().getQuantityByArea();
    final var firstOhCarryOver = firstOH.getResultingState().getCarryOver().getQuantityByArea();

    assertEquals(NO_AREA, firstOhProcessed.get(0).getArea());
    assertEquals(100.0, firstOhProcessed.get(0).getQuantity(), ALLOWED_DELTA);
    assertEquals(NO_AREA, firstOhCarryOver.get(0).getArea());
    assertEquals(80.0, firstOhCarryOver.get(0).getQuantity(), ALLOWED_DELTA);

    final var secondOH = results.get(1);
    final var secondOhProcessed = secondOH.getResultingState().getProcessed().getQuantityByArea();
    final var secondOhCarryOver = secondOH.getResultingState().getCarryOver().getQuantityByArea();
    assertEquals(NO_AREA, secondOhProcessed.get(0).getArea());
    assertEquals(130.0, secondOhProcessed.get(0).getQuantity(), ALLOWED_DELTA);
    assertEquals(NO_AREA, secondOhCarryOver.get(0).getArea());
    assertEquals(80.0, secondOhCarryOver.get(0).getQuantity(), ALLOWED_DELTA);
  }

  private void assertPicking(final List<ProjectionResult<BacklogByArea>> results) {
    final var firstOH = results.get(0);
    final var firstOhProcessed = firstOH.getResultingState().getProcessed().getQuantityByArea();
    final var firstOhCarryOver = firstOH.getResultingState().getCarryOver().getQuantityByArea();
    assertAreaValues(firstOhProcessed, 15.0, 10.0, 50.0, 25.0);
    assertAreaValues(firstOhCarryOver, 28.75, 10.0, 99.5, 16.75);

    final var secondOH = results.get(1);
    final var secondOhProcessed = secondOH.getResultingState().getProcessed().getQuantityByArea();
    final var secondOhCarryOver = secondOH.getResultingState().getCarryOver().getQuantityByArea();
    assertAreaValues(secondOhProcessed, 37.75, 12.25, 131.0, 19.0);
    assertAreaValues(secondOhCarryOver, 17.0, 4.25, 59.5, 4.25);
  }

  private void assertAreaValues(final List<QuantityAtArea> quantity, final double bl, final double hv, final double mz1, final double mz2) {
    assertEquals(4, quantity.size());

    assertEquals("BL", quantity.get(0).getArea());
    assertEquals(bl, quantity.get(0).getQuantity(), ALLOWED_DELTA);

    assertEquals("HV", quantity.get(1).getArea());
    assertEquals(hv, quantity.get(1).getQuantity(), ALLOWED_DELTA);

    assertEquals("MZ-1", quantity.get(2).getArea());
    assertEquals(mz1, quantity.get(2).getQuantity(), ALLOWED_DELTA);

    assertEquals("MZ-2", quantity.get(3).getArea());
    assertEquals(mz2, quantity.get(3).getQuantity(), ALLOWED_DELTA);
  }

  private BacklogMapper<BacklogBySla, BacklogByArea> getBacklogMapper() {
    final var slaA = Map.of(
        "BL", 0.15,
        "HV", 0.10,
        "MZ-1", 0.50,
        "MZ-2", 0.25
    );
    final var slaB = Map.of(
        "BL", 0.20,
        "HV", 0.05,
        "MZ-1", 0.70,
        "MZ-2", 0.05
    );
    final var slaC = Map.of(
        "BL", 0.0,
        "HV", 0.0,
        "MZ-1", 1.0,
        "MZ-2", 0.0
    );
    final var slaD = Map.of(
        "BL", 0.25,
        "HV", 0.25,
        "MZ-1", 0.25,
        "MZ-2", 0.25
    );

    final var distributions = Map.of(
        SLA_A, slaA,
        SLA_B, slaB,
        SLA_C, slaC,
        SLA_D, slaD
    );

    return new UnitsAreaDistributionMapper(Map.of(PICKING, distributions));
  }

  private Map<ProcessName, List<ProjectionResult<BacklogBySla>>> projectionResults() {

    final List<ProjectionResult<BacklogBySla>> wavingResults = List.of(
        new ProjectionResult<>(
            OP_HOUR_A,
            new SimpleProcessedBacklog<>(
                new BacklogBySla(List.of(
                    new QuantityAtDate(SLA_A, 70),
                    new QuantityAtDate(SLA_B, 30)
                )),
                new BacklogBySla(List.of(
                    new QuantityAtDate(SLA_A, 0),
                    new QuantityAtDate(SLA_B, 80)
                ))
            )
        ),
        new ProjectionResult<>(
            OP_HOUR_B,
            new SimpleProcessedBacklog<>(
                new BacklogBySla(List.of(
                    new QuantityAtDate(SLA_B, 130),
                    new QuantityAtDate(SLA_C, 0),
                    new QuantityAtDate(SLA_D, 0)
                )),
                new BacklogBySla(List.of(
                    new QuantityAtDate(SLA_B, 0),
                    new QuantityAtDate(SLA_C, 60),
                    new QuantityAtDate(SLA_D, 20)
                ))
            )
        )
    );

    final List<ProjectionResult<BacklogBySla>> pickingResults = List.of(
        new ProjectionResult<>(
            OP_HOUR_A,
            new SimpleProcessedBacklog<>(
                new BacklogBySla(List.of(
                    new QuantityAtDate(SLA_A, 100),
                    new QuantityAtDate(SLA_B, 0)
                )),
                new BacklogBySla(List.of(
                    new QuantityAtDate(SLA_A, 45),
                    new QuantityAtDate(SLA_B, 110)
                ))
            )
        ),
        new ProjectionResult<>(
            OP_HOUR_B,
            new SimpleProcessedBacklog<>(
                new BacklogBySla(List.of(
                    new QuantityAtDate(SLA_A, 45),
                    new QuantityAtDate(SLA_B, 155)
                )),
                new BacklogBySla(List.of(
                    new QuantityAtDate(SLA_A, 0),
                    new QuantityAtDate(SLA_B, 85)
                ))
            )
        )
    );

    final List<ProjectionResult<BacklogBySla>> packingResults = List.of(
        new ProjectionResult<>(
            OP_HOUR_A,
            new SimpleProcessedBacklog<>(
                new BacklogBySla(List.of(
                    new QuantityAtDate(SLA_A, 100),
                    new QuantityAtDate(SLA_B, 110),
                    new QuantityAtDate(SLA_C, 15)
                )),
                new BacklogBySla(List.of(
                    new QuantityAtDate(SLA_A, 0),
                    new QuantityAtDate(SLA_B, 0),
                    new QuantityAtDate(SLA_C, 60)
                ))
            )
        ),
        new ProjectionResult<>(
            OP_HOUR_B,
            new SimpleProcessedBacklog<>(
                new BacklogBySla(List.of(
                    new QuantityAtDate(SLA_A, 45),
                    new QuantityAtDate(SLA_B, 155),
                    new QuantityAtDate(SLA_C, 50)
                )),
                new BacklogBySla(List.of(
                    new QuantityAtDate(SLA_A, 0),
                    new QuantityAtDate(SLA_B, 0),
                    new QuantityAtDate(SLA_C, 10)
                ))
            )
        )
    );

    return Map.of(
        WAVING, wavingResults,
        PICKING, pickingResults,
        PACKING, packingResults
    );
  }

}
